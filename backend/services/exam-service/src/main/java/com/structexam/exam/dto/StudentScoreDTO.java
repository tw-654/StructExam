package com.structexam.exam.dto;

import java.time.LocalDateTime;

public class StudentScoreDTO {
    private Long recordId;
    private Long userId;
    private Long examId;
    private LocalDateTime enterTime;
    private LocalDateTime submitTime;
    private Integer score;
    private String status;
    private Integer submittedQuestionCount;
    private Integer judgedQuestionCount;
    private Integer acceptedQuestionCount;
    private String latestJudgeStatus;
    private Long latestJudgeTimeUsedMs;
    private LocalDateTime latestJudgeTime;

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
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

    public LocalDateTime getEnterTime() {
        return enterTime;
    }

    public void setEnterTime(LocalDateTime enterTime) {
        this.enterTime = enterTime;
    }

    public LocalDateTime getSubmitTime() {
        return submitTime;
    }

    public void setSubmitTime(LocalDateTime submitTime) {
        this.submitTime = submitTime;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getSubmittedQuestionCount() {
        return submittedQuestionCount;
    }

    public void setSubmittedQuestionCount(Integer submittedQuestionCount) {
        this.submittedQuestionCount = submittedQuestionCount;
    }

    public Integer getJudgedQuestionCount() {
        return judgedQuestionCount;
    }

    public void setJudgedQuestionCount(Integer judgedQuestionCount) {
        this.judgedQuestionCount = judgedQuestionCount;
    }

    public Integer getAcceptedQuestionCount() {
        return acceptedQuestionCount;
    }

    public void setAcceptedQuestionCount(Integer acceptedQuestionCount) {
        this.acceptedQuestionCount = acceptedQuestionCount;
    }

    public String getLatestJudgeStatus() {
        return latestJudgeStatus;
    }

    public void setLatestJudgeStatus(String latestJudgeStatus) {
        this.latestJudgeStatus = latestJudgeStatus;
    }

    public Long getLatestJudgeTimeUsedMs() {
        return latestJudgeTimeUsedMs;
    }

    public void setLatestJudgeTimeUsedMs(Long latestJudgeTimeUsedMs) {
        this.latestJudgeTimeUsedMs = latestJudgeTimeUsedMs;
    }

    public LocalDateTime getLatestJudgeTime() {
        return latestJudgeTime;
    }

    public void setLatestJudgeTime(LocalDateTime latestJudgeTime) {
        this.latestJudgeTime = latestJudgeTime;
    }
}
