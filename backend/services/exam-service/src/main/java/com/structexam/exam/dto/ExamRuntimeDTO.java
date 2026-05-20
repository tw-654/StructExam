package com.structexam.exam.dto;

import com.structexam.common.entity.ExamRecord;

import java.time.LocalDateTime;

public class ExamRuntimeDTO {
    private Long examId;
    private LocalDateTime serverTime;
    private LocalDateTime deadlineTime;
    private Long remainingSeconds;
    private ExamRecord record;

    public Long getExamId() {
        return examId;
    }

    public void setExamId(Long examId) {
        this.examId = examId;
    }

    public LocalDateTime getServerTime() {
        return serverTime;
    }

    public void setServerTime(LocalDateTime serverTime) {
        this.serverTime = serverTime;
    }

    public LocalDateTime getDeadlineTime() {
        return deadlineTime;
    }

    public void setDeadlineTime(LocalDateTime deadlineTime) {
        this.deadlineTime = deadlineTime;
    }

    public Long getRemainingSeconds() {
        return remainingSeconds;
    }

    public void setRemainingSeconds(Long remainingSeconds) {
        this.remainingSeconds = remainingSeconds;
    }

    public ExamRecord getRecord() {
        return record;
    }

    public void setRecord(ExamRecord record) {
        this.record = record;
    }
}
