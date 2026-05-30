package com.structexam.common.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.time.LocalDateTime;

/**
 * 判题用例明细。写入后不更新，故不继承 BaseEntity，无 updateTime。
 */
@TableName("t_judge_case_result")
public class JudgeCaseResult {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long judgeRecordId;

    /** 关联 t_question_test_case.id，自定义输入时为 null */
    private Long testCaseId;

    /** 本次判题内部序号，从 0 开始 */
    private Integer caseIndex;

    /** 用例名称快照 */
    private String caseName;

    /** AC / WA / TLE / MLE / RE / PE / SKIP */
    private String status;

    private Boolean passed;

    /** 是否对学生公开 input / expected；失败时前端按此决定是否显示详情 */
    private Boolean isPublic;

    /** 输入快照 */
    private String inputData;

    /** 期望输出快照 */
    private String expectedOutput;

    /** 实际输出 */
    private String actualOutput;

    private String errorMessage;

    private Long timeUsedMs;
    private Long memoryUsedKb;

    /** 本用例权重快照（对应 t_question_test_case.weight） */
    private Integer weight;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getJudgeRecordId() { return judgeRecordId; }
    public void setJudgeRecordId(Long judgeRecordId) { this.judgeRecordId = judgeRecordId; }

    public Long getTestCaseId() { return testCaseId; }
    public void setTestCaseId(Long testCaseId) { this.testCaseId = testCaseId; }

    public Integer getCaseIndex() { return caseIndex; }
    public void setCaseIndex(Integer caseIndex) { this.caseIndex = caseIndex; }

    public String getCaseName() { return caseName; }
    public void setCaseName(String caseName) { this.caseName = caseName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Boolean getPassed() { return passed; }
    public void setPassed(Boolean passed) { this.passed = passed; }

    public Boolean getIsPublic() { return isPublic; }
    public void setIsPublic(Boolean isPublic) { this.isPublic = isPublic; }

    public String getInputData() { return inputData; }
    public void setInputData(String inputData) { this.inputData = inputData; }

    public String getExpectedOutput() { return expectedOutput; }
    public void setExpectedOutput(String expectedOutput) { this.expectedOutput = expectedOutput; }

    public String getActualOutput() { return actualOutput; }
    public void setActualOutput(String actualOutput) { this.actualOutput = actualOutput; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Long getTimeUsedMs() { return timeUsedMs; }
    public void setTimeUsedMs(Long timeUsedMs) { this.timeUsedMs = timeUsedMs; }

    public Long getMemoryUsedKb() { return memoryUsedKb; }
    public void setMemoryUsedKb(Long memoryUsedKb) { this.memoryUsedKb = memoryUsedKb; }

    public Integer getWeight() { return weight; }
    public void setWeight(Integer weight) { this.weight = weight; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
