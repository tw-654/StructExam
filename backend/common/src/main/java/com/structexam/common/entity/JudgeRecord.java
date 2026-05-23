package com.structexam.common.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.time.LocalDateTime;

@TableName("t_judge_record")
public class JudgeRecord extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 分布式任务 ID，与 Redis judge:result:{taskId} 对应 */
    private String taskId;

    /** 关联 t_code_submission.id，RUN 类型可为 null */
    private Long submissionId;

    private Long examId;
    private Long userId;
    private Long questionId;
    private String language;

    /** 本次判题时的代码快照，防止提交记录被覆盖后无法追溯 */
    private String codeSnapshot;

    /** RUN / SUBMIT / SUBMIT_ALL / REJUDGE */
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

    /** sandbox-node 的 URI，便于问题追溯 */
    private String sandboxNode;

    private LocalDateTime startedTime;
    private LocalDateTime finishedTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public Long getSubmissionId() { return submissionId; }
    public void setSubmissionId(Long submissionId) { this.submissionId = submissionId; }

    public Long getExamId() { return examId; }
    public void setExamId(Long examId) { this.examId = examId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getCodeSnapshot() { return codeSnapshot; }
    public void setCodeSnapshot(String codeSnapshot) { this.codeSnapshot = codeSnapshot; }

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

    public String getSandboxNode() { return sandboxNode; }
    public void setSandboxNode(String sandboxNode) { this.sandboxNode = sandboxNode; }

    public LocalDateTime getStartedTime() { return startedTime; }
    public void setStartedTime(LocalDateTime startedTime) { this.startedTime = startedTime; }

    public LocalDateTime getFinishedTime() { return finishedTime; }
    public void setFinishedTime(LocalDateTime finishedTime) { this.finishedTime = finishedTime; }
}
