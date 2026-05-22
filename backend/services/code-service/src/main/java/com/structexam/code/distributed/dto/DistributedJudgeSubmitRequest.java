package com.structexam.code.distributed.dto;

import com.structexam.common.dto.TestCase;

import java.util.List;

public class DistributedJudgeSubmitRequest {
    private Long examId;
    private Long questionId;
    private String code;
    private String language;
    private List<TestCase> testCases;
    private Integer maxScore;
    private boolean persistResult;
    /** RUN / SUBMIT / SUBMIT_ALL / REJUDGE；空时默认 SUBMIT */
    private String triggerType;

    /** 关联的 t_code_submission.id，用于 t_judge_record.submission_id 回填 */
    private Long submissionId;
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
