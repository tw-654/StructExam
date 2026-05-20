package com.structexam.code.distributed.dto;

public class JudgeTaskResponse {
    private String taskId;
    private String status;

    public JudgeTaskResponse() {
    }

    public JudgeTaskResponse(String taskId, String status) {
        this.taskId = taskId;
        this.status = status;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
