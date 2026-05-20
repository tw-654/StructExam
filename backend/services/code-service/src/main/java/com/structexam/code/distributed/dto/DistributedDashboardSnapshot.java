package com.structexam.code.distributed.dto;

import java.util.List;

public class DistributedDashboardSnapshot {
    private long queueSize;
    private long processingQueueSize;
    private List<SandboxNodeView> nodes;
    private List<JudgeTaskSummary> recentTasks;
    private List<InteractiveSessionView> interactiveSessions;

    public long getQueueSize() {
        return queueSize;
    }

    public void setQueueSize(long queueSize) {
        this.queueSize = queueSize;
    }

    public long getProcessingQueueSize() {
        return processingQueueSize;
    }

    public void setProcessingQueueSize(long processingQueueSize) {
        this.processingQueueSize = processingQueueSize;
    }

    public List<SandboxNodeView> getNodes() {
        return nodes;
    }

    public void setNodes(List<SandboxNodeView> nodes) {
        this.nodes = nodes;
    }

    public List<JudgeTaskSummary> getRecentTasks() {
        return recentTasks;
    }

    public void setRecentTasks(List<JudgeTaskSummary> recentTasks) {
        this.recentTasks = recentTasks;
    }

    public List<InteractiveSessionView> getInteractiveSessions() {
        return interactiveSessions;
    }

    public void setInteractiveSessions(List<InteractiveSessionView> interactiveSessions) {
        this.interactiveSessions = interactiveSessions;
    }
}
