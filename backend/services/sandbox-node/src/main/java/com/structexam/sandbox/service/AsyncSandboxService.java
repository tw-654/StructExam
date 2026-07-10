package com.structexam.sandbox.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.structexam.common.dto.CodeExecuteRequest;
import com.structexam.common.dto.CodeExecuteResponse;
import com.structexam.common.dto.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class AsyncSandboxService {

    private static final Logger logger = LoggerFactory.getLogger(AsyncSandboxService.class);
    private static final String TASK_PREFIX = "sandbox:task:";
    private static final String RESULT_PREFIX = "sandbox:result:";
    private static final String QUEUE_KEY = "sandbox:task:queue";
    private static final long RESULT_TTL_HOURS = 24;

    private final SandboxPoolService sandboxPoolService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public AsyncSandboxService(SandboxPoolService sandboxPoolService, 
                              RedisTemplate<String, Object> redisTemplate,
                              ObjectMapper objectMapper) {
        this.sandboxPoolService = sandboxPoolService;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 异步提交代码执行任务
     * 立即返回任务ID，不等待执行完成
     */
    public String submitAsync(CodeExecuteRequest request) {
        String taskId = generateTaskId();
        
        try {
            // 存储任务信息
            TaskInfo taskInfo = new TaskInfo();
            taskInfo.setTaskId(taskId);
            taskInfo.setCode(request.getCode());
            taskInfo.setLanguage(request.getLanguage());
            taskInfo.setTestCases(request.getTestCases());
            taskInfo.setTimeout(request.getTimeout());
            taskInfo.setStatus(TaskStatus.PENDING);
            taskInfo.setSubmitTime(LocalDateTime.now());
            
            String taskKey = TASK_PREFIX + taskId;
            redisTemplate.opsForValue().set(taskKey, taskInfo, RESULT_TTL_HOURS, TimeUnit.HOURS);
            
            // 将任务加入队列
            redisTemplate.opsForList().rightPush(QUEUE_KEY, taskId);
            
            logger.info("Task {} submitted successfully", taskId);
            return taskId;
            
        } catch (Exception e) {
            logger.error("Failed to submit task", e);
            throw new RuntimeException("Failed to submit task: " + e.getMessage());
        }
    }

    /**
     * 查询任务结果
     */
    public TaskResult getTaskResult(String taskId) {
        try {
            String resultKey = RESULT_PREFIX + taskId;
            Object resultObj = redisTemplate.opsForValue().get(resultKey);
            
            if (resultObj == null) {
                // 检查任务是否存在
                String taskKey = TASK_PREFIX + taskId;
                Object taskObj = redisTemplate.opsForValue().get(taskKey);
                if (taskObj == null) {
                    return TaskResult.notFound(taskId);
                }
                
                // 任务存在但结果未生成
                TaskInfo taskInfo = (TaskInfo) taskObj;
                return TaskResult.pending(taskId, taskInfo.getStatus());
            }
            
            return (TaskResult) resultObj;
            
        } catch (Exception e) {
            logger.error("Failed to get task result: {}", taskId, e);
            return TaskResult.error(taskId, "Failed to get result: " + e.getMessage());
        }
    }

    /**
     * 异步执行任务（由调度器调用）
     */
    @Async("sandboxExecutor")
    public void executeTaskAsync(String taskId) {
        try {
            String taskKey = TASK_PREFIX + taskId;
            Object taskObj = redisTemplate.opsForValue().get(taskKey);
            
            if (taskObj == null) {
                logger.warn("Task {} not found", taskId);
                return;
            }
            
            TaskInfo taskInfo = (TaskInfo) taskObj;
            
            // 更新任务状态
            taskInfo.setStatus(TaskStatus.RUNNING);
            taskInfo.setStartTime(LocalDateTime.now());
            redisTemplate.opsForValue().set(taskKey, taskInfo, RESULT_TTL_HOURS, TimeUnit.HOURS);
            
            // 构建执行请求
            CodeExecuteRequest request = new CodeExecuteRequest();
            request.setCode(taskInfo.getCode());
            request.setLanguage(taskInfo.getLanguage());
            request.setTestCases(taskInfo.getTestCases());
            request.setTimeout(taskInfo.getTimeout());
            
            // 执行代码
            CodeExecuteResponse response = sandboxPoolService.execute(request);
            
            // 保存结果
            TaskResult taskResult = new TaskResult();
            taskResult.setTaskId(taskId);
            taskResult.setStatus(TaskStatus.COMPLETED);
            taskResult.setResponse(response);
            taskResult.setCompletedTime(LocalDateTime.now());
            taskResult.setExecutionTime(response.getExecutionTime());
            
            String resultKey = RESULT_PREFIX + taskId;
            redisTemplate.opsForValue().set(resultKey, taskResult, RESULT_TTL_HOURS, TimeUnit.HOURS);
            
            // 更新任务状态
            taskInfo.setStatus(TaskStatus.COMPLETED);
            taskInfo.setCompletedTime(LocalDateTime.now());
            redisTemplate.opsForValue().set(taskKey, taskInfo, RESULT_TTL_HOURS, TimeUnit.HOURS);
            
            logger.info("Task {} completed successfully", taskId);
            
        } catch (Exception e) {
            logger.error("Failed to execute task: {}", taskId, e);
            
            // 保存错误结果
            TaskResult taskResult = TaskResult.error(taskId, e.getMessage());
            String resultKey = RESULT_PREFIX + taskId;
            redisTemplate.opsForValue().set(resultKey, taskResult, RESULT_TTL_HOURS, TimeUnit.HOURS);
            
            // 更新任务状态
            try {
                String taskKey = TASK_PREFIX + taskId;
                Object taskObj = redisTemplate.opsForValue().get(taskKey);
                if (taskObj != null) {
                    TaskInfo taskInfo = (TaskInfo) taskObj;
                    taskInfo.setStatus(TaskStatus.FAILED);
                    taskInfo.setCompletedTime(LocalDateTime.now());
                    taskInfo.setErrorMessage(e.getMessage());
                    redisTemplate.opsForValue().set(taskKey, taskInfo, RESULT_TTL_HOURS, TimeUnit.HOURS);
                }
            } catch (Exception ex) {
                logger.error("Failed to update task status", ex);
            }
        }
    }

    private String generateTaskId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    // 任务状态枚举
    public enum TaskStatus {
        PENDING,    // 等待执行
        RUNNING,    // 执行中
        COMPLETED,  // 已完成
        FAILED      // 执行失败
    }

    // 任务信息
    public static class TaskInfo {
        private String taskId;
        private String code;
        private String language;
        private List<TestCase> testCases;
        private Long timeout;
        private TaskStatus status;
        private LocalDateTime submitTime;
        private LocalDateTime startTime;
        private LocalDateTime completedTime;
        private String errorMessage;

        // Getters and Setters
        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }
        public List<TestCase> getTestCases() { return testCases; }
        public void setTestCases(List<TestCase> testCases) { this.testCases = testCases; }
        public Long getTimeout() { return timeout; }
        public void setTimeout(Long timeout) { this.timeout = timeout; }
        public TaskStatus getStatus() { return status; }
        public void setStatus(TaskStatus status) { this.status = status; }
        public LocalDateTime getSubmitTime() { return submitTime; }
        public void setSubmitTime(LocalDateTime submitTime) { this.submitTime = submitTime; }
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        public LocalDateTime getCompletedTime() { return completedTime; }
        public void setCompletedTime(LocalDateTime completedTime) { this.completedTime = completedTime; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }

    // 任务结果
    public static class TaskResult {
        private String taskId;
        private TaskStatus status;
        private CodeExecuteResponse response;
        private LocalDateTime completedTime;
        private Long executionTime;
        private String errorMessage;

        public static TaskResult notFound(String taskId) {
            TaskResult result = new TaskResult();
            result.setTaskId(taskId);
            result.setStatus(TaskStatus.FAILED);
            result.setErrorMessage("Task not found");
            return result;
        }

        public static TaskResult pending(String taskId, TaskStatus status) {
            TaskResult result = new TaskResult();
            result.setTaskId(taskId);
            result.setStatus(status);
            return result;
        }

        public static TaskResult error(String taskId, String errorMessage) {
            TaskResult result = new TaskResult();
            result.setTaskId(taskId);
            result.setStatus(TaskStatus.FAILED);
            result.setErrorMessage(errorMessage);
            return result;
        }

        // Getters and Setters
        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
        public TaskStatus getStatus() { return status; }
        public void setStatus(TaskStatus status) { this.status = status; }
        public CodeExecuteResponse getResponse() { return response; }
        public void setResponse(CodeExecuteResponse response) { this.response = response; }
        public LocalDateTime getCompletedTime() { return completedTime; }
        public void setCompletedTime(LocalDateTime completedTime) { this.completedTime = completedTime; }
        public Long getExecutionTime() { return executionTime; }
        public void setExecutionTime(Long executionTime) { this.executionTime = executionTime; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
}