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

    /** t_judge_record.id，入队后由 JudgeRecordService.createPending 回填；RUN 类型为 null */
    private Long judgeRecordId;

    /** RUN / SUBMIT / SUBMIT_ALL / REJUDGE */
    private String triggerType;

    /** 关联的 t_code_submission.id，用于建立 judge_record → submission 外键 */
    private Long submissionId;

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

    public Long getJudgeRecordId() {
        return judgeRecordId;
    }

    public void setJudgeRecordId(Long judgeRecordId) {
        this.judgeRecordId = judgeRecordId;
    }

    public String getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
    }

    public Long getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(Long submissionId) {
        this.submissionId = submissionId;
    }
}
