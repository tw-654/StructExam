package com.structexam.code.distributed.dto;

import java.time.LocalDateTime;

public class JudgeTaskSummary {
    private String taskId;
    private Long userId;
    private Long examId;
    private Long questionId;
    private String language;
    private JudgeTaskStatus status;
    private int retryCount;
    private LocalDateTime submitTime;
    private LocalDateTime finishedTime;
    private String sandboxServiceId;
    private String sandboxNodeUri;
    private String error;

    public static JudgeTaskSummary queued(JudgeTask task) {
        JudgeTaskSummary summary = new JudgeTaskSummary();
        summary.setTaskId(task.getTaskId());
        summary.setUserId(task.getUserId());
        summary.setExamId(task.getExamId());
        summary.setQuestionId(task.getQuestionId());
        summary.setLanguage(task.getLanguage());
        summary.setStatus(JudgeTaskStatus.WAIT);
        summary.setRetryCount(task.getRetryCount());
        summary.setSubmitTime(task.getSubmitTime());
        return summary;
    }

    public static JudgeTaskSummary finished(JudgeResult result) {
        JudgeTaskSummary summary = new JudgeTaskSummary();
        summary.setTaskId(result.getTaskId());
        summary.setUserId(result.getUserId());
        summary.setExamId(result.getExamId());
        summary.setQuestionId(result.getQuestionId());
        summary.setStatus(result.getStatus());
        summary.setFinishedTime(result.getFinishedTime());
        summary.setSandboxServiceId(result.getSandboxServiceId());
        summary.setSandboxNodeUri(result.getSandboxNodeUri());
        summary.setError(result.getError());
        return summary;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getExamId() {
        return examId;
    }

    public void setExamId(Long examId) {
        this.examId = examId;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public JudgeTaskStatus getStatus() {
        return status;
    }

    public void setStatus(JudgeTaskStatus status) {
        this.status = status;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public LocalDateTime getSubmitTime() {
        return submitTime;
    }

    public void setSubmitTime(LocalDateTime submitTime) {
        this.submitTime = submitTime;
    }

    public LocalDateTime getFinishedTime() {
        return finishedTime;
    }

    public void setFinishedTime(LocalDateTime finishedTime) {
        this.finishedTime = finishedTime;
    }

    public String getSandboxServiceId() {
        return sandboxServiceId;
    }

    public void setSandboxServiceId(String sandboxServiceId) {
        this.sandboxServiceId = sandboxServiceId;
    }

    public String getSandboxNodeUri() {
        return sandboxNodeUri;
    }

    public void setSandboxNodeUri(String sandboxNodeUri) {
        this.sandboxNodeUri = sandboxNodeUri;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
