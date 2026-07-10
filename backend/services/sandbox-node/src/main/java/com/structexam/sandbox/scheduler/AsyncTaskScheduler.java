package com.structexam.sandbox.scheduler;

import com.structexam.sandbox.service.AsyncSandboxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class AsyncTaskScheduler {

    private static final Logger logger = LoggerFactory.getLogger(AsyncTaskScheduler.class);
    private static final String QUEUE_KEY = "sandbox:task:queue";
    private static final int MAX_TASKS_PER_CYCLE = 10;

    private final AsyncSandboxService asyncSandboxService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public AsyncTaskScheduler(AsyncSandboxService asyncSandboxService,
                              RedisTemplate<String, Object> redisTemplate) {
        this.asyncSandboxService = asyncSandboxService;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 定时从队列中获取任务并执行
     * 每100毫秒执行一次，每次最多处理10个任务
     */
    @Scheduled(fixedDelay = 100)
    public void processTasks() {
        try {
            int processedCount = 0;
            
            while (processedCount < MAX_TASKS_PER_CYCLE) {
                // 从队列左侧获取任务（FIFO）
                Object taskIdObj = redisTemplate.opsForList().leftPop(QUEUE_KEY);
                
                if (taskIdObj == null) {
                    // 队列为空，退出循环
                    break;
                }
                
                String taskId = taskIdObj.toString();
                logger.debug("Processing task: {}", taskId);
                
                // 异步执行任务
                asyncSandboxService.executeTaskAsync(taskId);
                processedCount++;
            }
            
            if (processedCount > 0) {
                logger.debug("Processed {} tasks in this cycle", processedCount);
            }
            
        } catch (Exception e) {
            logger.error("Error processing tasks from queue", e);
        }
    }

    /**
     * 定期清理过期的任务和结果
     * 每小时执行一次
     */
    @Scheduled(fixedDelay = 3600000)
    public void cleanupExpiredTasks() {
        try {
            logger.info("Starting cleanup of expired tasks");
            
            // 这里可以添加清理逻辑，比如删除超过24小时的任务
            // Redis的TTL会自动处理大部分清理工作
            
            logger.info("Cleanup of expired tasks completed");
            
        } catch (Exception e) {
            logger.error("Error during cleanup of expired tasks", e);
        }
    }

    /**
     * 监控队列状态
     * 每30秒记录一次队列长度
     */
    @Scheduled(fixedDelay = 30000)
    public void monitorQueue() {
        try {
            Long queueSize = redisTemplate.opsForList().size(QUEUE_KEY);
            if (queueSize != null && queueSize > 0) {
                logger.info("Current queue size: {}", queueSize);
            }
        } catch (Exception e) {
            logger.error("Error monitoring queue", e);
        }
    }
}