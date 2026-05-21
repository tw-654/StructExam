package com.structexam.code.distributed.service;

import com.structexam.code.distributed.config.DistributedJudgeProperties;
import com.structexam.code.distributed.dto.DistributedJudgeSubmitRequest;
import com.structexam.code.distributed.dto.ExamSubmitResponse;
import com.structexam.code.distributed.dto.JudgeTask;
import com.structexam.code.distributed.dto.JudgeTaskResponse;
import com.structexam.code.service.JudgeRecordService;
import com.structexam.common.entity.JudgeRecord;
import com.structexam.common.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class DistributedJudgeService {

    private final RedisDistributedLockService lockService;
    private final JudgeTaskQueueService queueService;
    private final DistributedJudgeProperties properties;
    private final JudgeRecordService judgeRecordService;

    public DistributedJudgeService(RedisDistributedLockService lockService,
                                   JudgeTaskQueueService queueService,
                                   DistributedJudgeProperties properties,
                                   JudgeRecordService judgeRecordService) {
        this.lockService = lockService;
        this.queueService = queueService;
        this.properties = properties;
        this.judgeRecordService = judgeRecordService;
    }

    public JudgeTaskResponse submit(Long userId, DistributedJudgeSubmitRequest request) {
        validateSubmitRequest(userId, request);
        String lockKey = properties.getLockKeyPrefix() + userId + ":" + request.getExamId() + ":" + request.getQuestionId();
        String token = lockService.tryLock(lockKey, properties.getJudgeLockTtl())
                .orElseThrow(() -> new BusinessException(409, "判题进行中，请勿重复提交"));

        try {
            JudgeTask task = new JudgeTask();
            task.setTaskId(UUID.randomUUID().toString());
            task.setUserId(userId);
            task.setExamId(request.getExamId());
            task.setQuestionId(request.getQuestionId());
            task.setCode(request.getCode());
            task.setLanguage(StringUtils.hasText(request.getLanguage()) ? request.getLanguage() : "java");
            task.setTestCases(request.getTestCases());
            task.setMaxScore(request.getMaxScore());
            task.setPersistResult(request.isPersistResult());
            task.setSubmitTime(LocalDateTime.now());
            task.setRetryCount(0);
            task.setLockKey(lockKey);
            task.setLockToken(token);
            task.setSubmissionId(request.getSubmissionId());
            String triggerType = StringUtils.hasText(request.getTriggerType())
                    ? request.getTriggerType() : "SUBMIT";
            task.setTriggerType(triggerType);

            // 仅官方提交（persistResult=true）才落库，纯"运行"不占存储
            if (request.isPersistResult()) {
                JudgeRecord pending = judgeRecordService.createPending(task, triggerType);
                task.setJudgeRecordId(pending.getId());
            }

            queueService.enqueue(task);
            return new JudgeTaskResponse(task.getTaskId(), "queued");
        } catch (RuntimeException ex) {
            lockService.release(lockKey, token);
            throw ex;
        }
    }

    public ExamSubmitResponse submitExam(Long userId, Long examId, String triggerType) {
        String lockKey = properties.getExamSubmitLockKeyPrefix() + examId + ":" + userId;
        String token = lockService.tryLock(lockKey, properties.getExamSubmitLockTtl())
                .orElseThrow(() -> new BusinessException(409, "交卷中，请稍后"));

        try {
            ExamSubmitResponse response = new ExamSubmitResponse();
            response.setExamId(examId);
            response.setUserId(userId);
            response.setTriggerType(StringUtils.hasText(triggerType) ? triggerType : "manual");
            response.setStatus("accepted");
            response.setSubmitTime(LocalDateTime.now());
            return response;
        } finally {
            lockService.release(lockKey, token);
        }
    }

    private void validateSubmitRequest(Long userId, DistributedJudgeSubmitRequest request) {
        if (userId == null) {
            throw new BusinessException(401, "Missing user identity");
        }
        if (request == null || request.getExamId() == null || request.getQuestionId() == null) {
            throw new BusinessException(400, "examId and questionId are required");
        }
        if (!StringUtils.hasText(request.getCode())) {
            throw new BusinessException(400, "code is required");
        }
    }
}
