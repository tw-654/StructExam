package com.structexam.code.distributed.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.structexam.code.mapper.CodeSubmissionMapper;
import com.structexam.code.mapper.JudgeRecordMapper;
import com.structexam.common.entity.JudgeRecord;
import com.structexam.code.service.JudgeRecordService;
import com.structexam.code.distributed.config.DistributedJudgeProperties;
import com.structexam.code.distributed.dto.JudgeResult;
import com.structexam.code.distributed.dto.JudgeTaskStatus;
import com.structexam.code.distributed.dto.JudgeTask;
import com.structexam.code.distributed.dto.QueuedJudgeTask;
import com.structexam.common.dto.CodeExecuteRequest;
import com.structexam.common.dto.CodeExecuteResponse;
import com.structexam.common.entity.CodeSubmission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

@Service
public class DistributedJudgeScheduler {

    private static final Logger logger = LoggerFactory.getLogger(DistributedJudgeScheduler.class);

    private final JudgeTaskQueueService queueService;
    private final SandboxNodeRegistry nodeRegistry;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final DistributedJudgeProperties properties;
    private final Executor distributedJudgeExecutor;
    private final RedisDistributedLockService lockService;
    private final CodeSubmissionMapper codeSubmissionMapper;
    private final JudgeRecordMapper judgeRecordMapper;
    private final JudgeRecordService judgeRecordService;

    public DistributedJudgeScheduler(JudgeTaskQueueService queueService,
                                     SandboxNodeRegistry nodeRegistry,
                                     RestTemplate distributedJudgeRestTemplate,
                                     ObjectMapper objectMapper,
                                     DistributedJudgeProperties properties,
                                     Executor distributedJudgeExecutor,
                                     RedisDistributedLockService lockService,
                                     CodeSubmissionMapper codeSubmissionMapper,
                                     JudgeRecordMapper judgeRecordMapper,
                                     JudgeRecordService judgeRecordService) {
        this.queueService = queueService;
        this.nodeRegistry = nodeRegistry;
        this.restTemplate = distributedJudgeRestTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.distributedJudgeExecutor = distributedJudgeExecutor;
        this.lockService = lockService;
        this.codeSubmissionMapper = codeSubmissionMapper;
        this.judgeRecordMapper = judgeRecordMapper;
        this.judgeRecordService = judgeRecordService;
    }

    @Scheduled(fixedDelayString = "${distributed.judge.scheduler-delay-ms:100}")
    public void dispatchOnce() {
        if (!properties.isSchedulerEnabled()) {
            return;
        }

        for (int i = 0; i < properties.getSchedulerBatchSize(); i++) {
            Optional<QueuedJudgeTask> taskOptional = queueService.dequeue();
            if (taskOptional.isEmpty()) {
                return;
            }
            dispatchTask(taskOptional.get());
        }
    }

    private void dispatchTask(QueuedJudgeTask queuedTask) {
        JudgeTask task = queuedTask.getTask();
        Optional<ServiceInstance> nodeOptional = selectNodeByStrategy();
        if (nodeOptional.isEmpty()) {
            if (nodeRegistry.hasHealthyNodeIgnoringLoad()) {
                queueService.requeue(task, queuedTask.getPayload());
                return;
            }
            retryOrFail(task, queuedTask.getPayload(), "No healthy sandbox node available");
            return;
        }

        ServiceInstance node = nodeOptional.get();
        nodeRegistry.incrementRunningTasks(node);
        try {
            distributedJudgeExecutor.execute(() -> executeReservedTask(task, queuedTask.getPayload(), node));
        } catch (RejectedExecutionException ex) {
            nodeRegistry.decrementRunningTasks(node);
            queueService.requeue(task, queuedTask.getPayload());
        }
    }

    private Optional<ServiceInstance> selectNodeByStrategy() {
        String strategy = properties.getLoadBalanceStrategy();
        if ("leastTasks".equalsIgnoreCase(strategy)) {
            return nodeRegistry.selectLeastTasks();
        } else {
            return nodeRegistry.selectRoundRobin();
        }
    }

    private void executeReservedTask(JudgeTask task, String payload, ServiceInstance node) {
        try {
            CodeExecuteResponse response = executeOnNode(node, task);
            nodeRegistry.markSuccess(node);
            JudgeResult result = JudgeResultMapper.fromExecution(task, response);
            result.setSandboxServiceId(node.getServiceId());
            result.setSandboxNodeUri(node.getUri().toString());
            queueService.saveResult(result);
            if (task.isPersistResult()) {
                judgeRecordService.completeJudge(task, result);
                persistJudgeResult(task, result);
            }
            queueService.ack(payload);
            releaseSubmitLock(task);
            logger.info("Judge task {} finished with status {}", task.getTaskId(), result.getStatus());
        } catch (RestClientException ex) {
            nodeRegistry.markFailure(node);
            retryOrFail(task, payload, ex.getMessage());
        } catch (RuntimeException ex) {
            retryOrFail(task, payload, ex.getMessage());
        } finally {
            nodeRegistry.decrementRunningTasks(node);
        }
    }

    private CodeExecuteResponse executeOnNode(ServiceInstance node, JudgeTask task) {
        CodeExecuteRequest request = new CodeExecuteRequest();
        request.setCode(task.getCode());
        request.setLanguage(task.getLanguage());
        request.setTestCases(task.getTestCases());
        request.setTimeout(properties.getDefaultRunTimeoutSeconds());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        URI uri = node.getUri().resolve(properties.getSandboxRunPath());
        String payload = restTemplate.postForObject(uri, new HttpEntity<>(request, headers), String.class);
        return readExecutionResponse(payload);
    }

    private CodeExecuteResponse readExecutionResponse(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode data = root.has("data") ? root.get("data") : root;
            return objectMapper.treeToValue(data, CodeExecuteResponse.class);
        } catch (Exception e) {
            throw new IllegalStateException("Invalid sandbox response: " + e.getMessage(), e);
        }
    }

    private void retryOrFail(JudgeTask task, String payload, String reason) {
        int nextRetry = task.getRetryCount() + 1;
        task.setRetryCount(nextRetry);
        if (nextRetry >= properties.getMaxRetryCount()) {
            JudgeResult result = JudgeResultMapper.failed(task, reason);
            queueService.saveResult(result);
            if (task.isPersistResult()) {
                judgeRecordService.completeJudge(task, result);
                persistJudgeResult(task, result);
            }
            queueService.ack(payload);
            releaseSubmitLock(task);
            logger.warn("Judge task {} failed after {} retries: {}", task.getTaskId(), nextRetry, reason);
            return;
        }
        queueService.requeue(task, payload);
        logger.warn("Judge task {} requeued, retry {}/{}: {}",
                task.getTaskId(), nextRetry, properties.getMaxRetryCount(), reason);
    }

    private void releaseSubmitLock(JudgeTask task) {
        if (task.getLockKey() != null && task.getLockToken() != null) {
            lockService.release(task.getLockKey(), task.getLockToken());
        }
    }

    private void persistJudgeResult(JudgeTask task, JudgeResult result) {
        CodeSubmission submission = codeSubmissionMapper.selectOne(
                new LambdaQueryWrapper<CodeSubmission>()
                        .eq(CodeSubmission::getExamId, task.getExamId())
                        .eq(CodeSubmission::getUserId, task.getUserId())
                        .eq(CodeSubmission::getQuestionId, task.getQuestionId())
        );
        if (submission == null) {
            submission = new CodeSubmission();
            submission.setExamId(task.getExamId());
            submission.setUserId(task.getUserId());
            submission.setQuestionId(task.getQuestionId());
            submission.setCodeContent(task.getCode());
            submission.setLanguage(task.getLanguage());
            submission.setSubmitTime(task.getSubmitTime() == null ? LocalDateTime.now() : task.getSubmitTime());
        } else {
            submission.setCodeContent(task.getCode());
            submission.setLanguage(task.getLanguage());
            if (submission.getSubmitTime() == null) {
                submission.setSubmitTime(task.getSubmitTime() == null ? LocalDateTime.now() : task.getSubmitTime());
            }
        }
        submission.setStatus("GRADED");
        submission.setJudgeStatus(result.getStatus() == null ? null : result.getStatus().name());
        submission.setTimeUsedMs(result.getTimeUsedMs());
        submission.setMemoryUsedKb(result.getMemoryUsedKb());
        submission.setJudgeTime(result.getFinishedTime() == null ? LocalDateTime.now() : result.getFinishedTime());
        JudgeRecord judgeRecord = judgeRecordMapper.selectOne(
                new LambdaQueryWrapper<JudgeRecord>().eq(JudgeRecord::getTaskId, task.getTaskId()));
        if (judgeRecord != null) {
            submission.setScore(judgeRecord.getScore() != null ? judgeRecord.getScore() : 0);
            if (judgeRecord.getJudgeStatus() != null) {
                submission.setJudgeStatus(judgeRecord.getJudgeStatus());
            }
        } else {
            submission.setScore(0);
        }
        submission.setJudgeMessage(result.getError());

        if (submission.getId() == null) {
            codeSubmissionMapper.insert(submission);
        } else {
            codeSubmissionMapper.updateById(submission);
        }
    }
}
