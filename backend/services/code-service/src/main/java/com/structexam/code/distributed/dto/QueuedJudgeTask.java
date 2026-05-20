package com.structexam.code.distributed.dto;

public class QueuedJudgeTask {
    private final JudgeTask task;
    private final String payload;

    public QueuedJudgeTask(JudgeTask task, String payload) {
        this.task = task;
        this.payload = payload;
    }

    public JudgeTask getTask() {
        return task;
    }

    public String getPayload() {
        return payload;
    }
}
