package com.structexam.code.distributed.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.structexam.code.distributed.config.DistributedJudgeProperties;
import com.structexam.code.distributed.dto.JudgeResult;
import com.structexam.code.distributed.dto.JudgeTask;
import com.structexam.code.distributed.dto.JudgeTaskSummary;
import com.structexam.code.distributed.dto.QueuedJudgeTask;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class JudgeTaskQueueService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final DistributedJudgeProperties properties;
    private final DefaultRedisScript<String> dequeueScript;

    public JudgeTaskQueueService(RedisTemplate<String, Object> redisTemplate,
                                 ObjectMapper objectMapper,
                                 DistributedJudgeProperties properties) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.dequeueScript = new DefaultRedisScript<>();
        this.dequeueScript.setResultType(String.class);
        this.dequeueScript.setScriptText(
                "local v = redis.call('lpop', KEYS[1]); " +
                        "if v then redis.call('rpush', KEYS[2], v); end; " +
                        "return v;"
        );
    }

    public void enqueue(JudgeTask task) {
        redisTemplate.opsForList().rightPush(properties.getQueueKey(), toJson(task));
        saveSummary(JudgeTaskSummary.queued(task));
    }

    public Optional<QueuedJudgeTask> dequeue() {
        String value = redisTemplate.execute(
                dequeueScript,
                List.of(properties.getQueueKey(), properties.getProcessingQueueKey())
        );
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(new QueuedJudgeTask(fromJson(value, JudgeTask.class), value));
    }

    public void ack(String payload) {
        redisTemplate.opsForList().remove(properties.getProcessingQueueKey(), 1, payload);
    }

    public void requeue(JudgeTask task, String oldPayload) {
        ack(oldPayload);
        enqueue(task);
    }

    public long size() {
        Long size = redisTemplate.opsForList().size(properties.getQueueKey());
        return size != null ? size : 0L;
    }

    public long processingSize() {
        Long size = redisTemplate.opsForList().size(properties.getProcessingQueueKey());
        return size != null ? size : 0L;
    }

    public long recoverProcessingTasks() {
        long recovered = 0L;
        Object value;
        while ((value = redisTemplate.opsForList().leftPop(properties.getProcessingQueueKey())) != null) {
            redisTemplate.opsForList().rightPush(properties.getQueueKey(), value);
            recovered++;
        }
        return recovered;
    }

    public void saveResult(JudgeResult result) {
        redisTemplate.opsForValue().set(resultKey(result.getTaskId()), toJson(result), properties.getResultTtl());
        mergeFinishedSummary(result);
    }

    public Optional<JudgeResult> getResult(String taskId) {
        Object value = redisTemplate.opsForValue().get(resultKey(taskId));
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(fromJson(value.toString(), JudgeResult.class));
    }

    private String resultKey(String taskId) {
        return properties.getResultKeyPrefix() + taskId;
    }

    public List<JudgeTaskSummary> recentTasks() {
        List<Object> ids = redisTemplate.opsForList().range(
                properties.getRecentTaskListKey(), 0, properties.getRecentTaskLimit() - 1);
        if (ids == null) {
            return List.of();
        }
        return ids.stream()
                .map(Object::toString)
                .map(this::getSummary)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    private void mergeFinishedSummary(JudgeResult result) {
        JudgeTaskSummary finished = JudgeTaskSummary.finished(result);
        Optional<JudgeTaskSummary> existing = getSummary(result.getTaskId());
        if (existing.isPresent()) {
            JudgeTaskSummary summary = existing.get();
            summary.setStatus(finished.getStatus());
            summary.setFinishedTime(finished.getFinishedTime());
            summary.setSandboxServiceId(finished.getSandboxServiceId());
            summary.setSandboxNodeUri(finished.getSandboxNodeUri());
            summary.setError(finished.getError());
            saveSummary(summary);
            return;
        }
        saveSummary(finished);
    }

    private Optional<JudgeTaskSummary> getSummary(String taskId) {
        Object value = redisTemplate.opsForValue().get(summaryKey(taskId));
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(fromJson(value.toString(), JudgeTaskSummary.class));
    }

    private void saveSummary(JudgeTaskSummary summary) {
        redisTemplate.opsForValue().set(summaryKey(summary.getTaskId()), toJson(summary), properties.getResultTtl());
        redisTemplate.opsForList().remove(properties.getRecentTaskListKey(), 0, summary.getTaskId());
        redisTemplate.opsForList().leftPush(properties.getRecentTaskListKey(), summary.getTaskId());
        redisTemplate.opsForList().trim(properties.getRecentTaskListKey(), 0, properties.getRecentTaskLimit() - 1);
    }

    private String summaryKey(String taskId) {
        return properties.getTaskSummaryKeyPrefix() + taskId;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize judge payload", e);
        }
    }

    private <T> T fromJson(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize judge payload", e);
        }
    }
}
