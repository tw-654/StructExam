package com.structexam.common.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.time.LocalDateTime;

@TableName("t_code_submission")
public class CodeSubmission extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long examId;

    private Long userId;

    private Long questionId;

    private String codeContent;

    private String language;

    private String status;

    private LocalDateTime submitTime;

    private Integer score;

    private String judgeStatus;

    private Long timeUsedMs;

    private Long memoryUsedKb;

    private LocalDateTime judgeTime;

    private String judgeMessage;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getExamId() {
        return examId;
    }

    public void setExamId(Long examId) {
        this.examId = examId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public String getCodeContent() {
        return codeContent;
    }

    public void setCodeContent(String codeContent) {
        this.codeContent = codeContent;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public String getJudgeStatus() {
        return judgeStatus;
    }

    public void setJudgeStatus(String judgeStatus) {
        this.judgeStatus = judgeStatus;
    }

    public Long getTimeUsedMs() {
        return timeUsedMs;
    }

    public void setTimeUsedMs(Long timeUsedMs) {
        this.timeUsedMs = timeUsedMs;
    }

    public Long getMemoryUsedKb() {
        return memoryUsedKb;
    }

    public void setMemoryUsedKb(Long memoryUsedKb) {
        this.memoryUsedKb = memoryUsedKb;
    }

    public LocalDateTime getJudgeTime() {
        return judgeTime;
    }

    public void setJudgeTime(LocalDateTime judgeTime) {
        this.judgeTime = judgeTime;
    }

    public String getJudgeMessage() {
        return judgeMessage;
    }

    public void setJudgeMessage(String judgeMessage) {
        this.judgeMessage = judgeMessage;
    }
}
