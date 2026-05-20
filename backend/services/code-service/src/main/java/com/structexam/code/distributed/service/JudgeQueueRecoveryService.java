package com.structexam.code.distributed.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class JudgeQueueRecoveryService {

    private static final Logger logger = LoggerFactory.getLogger(JudgeQueueRecoveryService.class);

    private final JudgeTaskQueueService queueService;

    public JudgeQueueRecoveryService(JudgeTaskQueueService queueService) {
        this.queueService = queueService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recoverProcessingQueue() {
        long recovered = queueService.recoverProcessingTasks();
        if (recovered > 0) {
            logger.warn("Recovered {} judge task(s) from processing queue", recovered);
        }
    }
}
