package com.structexam.code.distributed.dto;

import com.structexam.common.dto.CodeExecuteResponse;

import java.time.LocalDateTime;
import java.util.List;

public class JudgeResult {
    private String taskId;
    private Long userId;
    private Long examId;
    private Long questionId;
    private JudgeTaskStatus status;
    private Long timeUsedMs;
    private Long memoryUsedKb;
    private String output;
    private String error;
    private String sandboxServiceId;
    private String sandboxNodeUri;
    private List<CodeExecuteResponse.TestResult> testCaseResults;
    private LocalDateTime finishedTime;

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

    public JudgeTaskStatus getStatus() {
        return status;
    }

    public void setStatus(JudgeTaskStatus status) {
        this.status = status;
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

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
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

    public List<CodeExecuteResponse.TestResult> getTestCaseResults() {
        return testCaseResults;
    }

    public void setTestCaseResults(List<CodeExecuteResponse.TestResult> testCaseResults) {
        this.testCaseResults = testCaseResults;
    }

    public LocalDateTime getFinishedTime() {
        return finishedTime;
    }

    public void setFinishedTime(LocalDateTime finishedTime) {
        this.finishedTime = finishedTime;
    }
}
