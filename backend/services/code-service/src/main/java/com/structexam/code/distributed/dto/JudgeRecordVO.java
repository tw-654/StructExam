package com.structexam.code.distributed.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 判题记录视图对象，包含聚合结果与逐条用例明细。
 */
public class JudgeRecordVO {

    private Long id;
    private String taskId;
    private Long examId;
    private Long userId;
    private Long questionId;
    private String language;
    private String triggerType;

    /** AC / WA / CE / RE / TLE / MLE / PE / FAILED / JUDGING */
    private String judgeStatus;

    private Integer totalCases;
    private Integer passedCases;
    private Integer score;
    private Integer maxScore;

    private Long timeUsedMs;
    private Long memoryUsedKb;

    private String compileError;
    private String runtimeError;
    private String judgeMessage;

    private LocalDateTime finishedTime;

    /** 逐条测例结果，学生视角下非公开用例的详情字段会被脱敏 */
    private List<JudgeCaseResultVO> cases;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public Long getExamId() { return examId; }
    public void setExamId(Long examId) { this.examId = examId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getTriggerType() { return triggerType; }
    public void setTriggerType(String triggerType) { this.triggerType = triggerType; }

    public String getJudgeStatus() { return judgeStatus; }
    public void setJudgeStatus(String judgeStatus) { this.judgeStatus = judgeStatus; }

    public Integer getTotalCases() { return totalCases; }
    public void setTotalCases(Integer totalCases) { this.totalCases = totalCases; }

    public Integer getPassedCases() { return passedCases; }
    public void setPassedCases(Integer passedCases) { this.passedCases = passedCases; }

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }

    public Integer getMaxScore() { return maxScore; }
    public void setMaxScore(Integer maxScore) { this.maxScore = maxScore; }

    public Long getTimeUsedMs() { return timeUsedMs; }
    public void setTimeUsedMs(Long timeUsedMs) { this.timeUsedMs = timeUsedMs; }

    public Long getMemoryUsedKb() { return memoryUsedKb; }
    public void setMemoryUsedKb(Long memoryUsedKb) { this.memoryUsedKb = memoryUsedKb; }

    public String getCompileError() { return compileError; }
    public void setCompileError(String compileError) { this.compileError = compileError; }

    public String getRuntimeError() { return runtimeError; }
    public void setRuntimeError(String runtimeError) { this.runtimeError = runtimeError; }

    public String getJudgeMessage() { return judgeMessage; }
    public void setJudgeMessage(String judgeMessage) { this.judgeMessage = judgeMessage; }

    public LocalDateTime getFinishedTime() { return finishedTime; }
    public void setFinishedTime(LocalDateTime finishedTime) { this.finishedTime = finishedTime; }

    public List<JudgeCaseResultVO> getCases() { return cases; }
    public void setCases(List<JudgeCaseResultVO> cases) { this.cases = cases; }
}
