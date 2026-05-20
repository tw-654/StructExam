package com.structexam.code.distributed.dto;

import java.time.LocalDateTime;
import java.util.List;

public class LoadTestResponse {
    private int requestedTasks;
    private int acceptedTasks;
    private int rejectedTasks;
    private LocalDateTime startedAt;
    private List<String> taskIds;

    public int getRequestedTasks() {
        return requestedTasks;
    }

    public void setRequestedTasks(int requestedTasks) {
        this.requestedTasks = requestedTasks;
    }

    public int getAcceptedTasks() {
        return acceptedTasks;
    }

    public void setAcceptedTasks(int acceptedTasks) {
        this.acceptedTasks = acceptedTasks;
    }

    public int getRejectedTasks() {
        return rejectedTasks;
    }

    public void setRejectedTasks(int rejectedTasks) {
        this.rejectedTasks = rejectedTasks;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public List<String> getTaskIds() {
        return taskIds;
    }

    public void setTaskIds(List<String> taskIds) {
        this.taskIds = taskIds;
    }
}
