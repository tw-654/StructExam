package com.structexam.code.distributed.dto;

import com.structexam.common.dto.TestCase;

import java.time.LocalDateTime;
import java.util.List;

public class JudgeTask {
    private String taskId;
    private Long userId;
    private Long examId;
    private Long questionId;
    private String code;
    private String language;
    private List<TestCase> testCases;
    private Integer maxScore;
    private boolean persistResult;
    private LocalDateTime submitTime;
    private int retryCount;
    private String lockKey;
    private String lockToken;

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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public List<TestCase> getTestCases() {
        return testCases;
    }

    public void setTestCases(List<TestCase> testCases) {
        this.testCases = testCases;
    }

    public Integer getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(Integer maxScore) {
        this.maxScore = maxScore;
    }

    public boolean isPersistResult() {
        return persistResult;
    }

    public void setPersistResult(boolean persistResult) {
        this.persistResult = persistResult;
    }

    public LocalDateTime getSubmitTime() {
        return submitTime;
    }

    public void setSubmitTime(LocalDateTime submitTime) {
        this.submitTime = submitTime;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public String getLockKey() {
        return lockKey;
    }

    public void setLockKey(String lockKey) {
        this.lockKey = lockKey;
    }

    public String getLockToken() {
        return lockToken;
    }

    public void setLockToken(String lockToken) {
        this.lockToken = lockToken;
    }
}
