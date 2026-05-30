package com.structexam.common.entity;

import com.baomidou.mybatisplus.annotation.*;

@TableName("t_question_test_case")
public class QuestionTestCase extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long questionId;

    private String caseName;

    private String inputData;

    private String expectedOutput;

    private Boolean isSample;

    private Boolean isPublic;

    private Integer weight;

    private Integer timeLimitMs;

    private Integer memoryLimitKb;

    private Integer sortOrder;

    /** 1=启用 0=禁用 */
    private Integer status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }

    public String getCaseName() { return caseName; }
    public void setCaseName(String caseName) { this.caseName = caseName; }

    public String getInputData() { return inputData; }
    public void setInputData(String inputData) { this.inputData = inputData; }

    public String getExpectedOutput() { return expectedOutput; }
    public void setExpectedOutput(String expectedOutput) { this.expectedOutput = expectedOutput; }

    public Boolean getIsSample() { return isSample; }
    public void setIsSample(Boolean isSample) { this.isSample = isSample; }

    public Boolean getIsPublic() { return isPublic; }
    public void setIsPublic(Boolean isPublic) { this.isPublic = isPublic; }

    public Integer getWeight() { return weight; }
    public void setWeight(Integer weight) { this.weight = weight; }

    public Integer getTimeLimitMs() { return timeLimitMs; }
    public void setTimeLimitMs(Integer timeLimitMs) { this.timeLimitMs = timeLimitMs; }

    public Integer getMemoryLimitKb() { return memoryLimitKb; }
    public void setMemoryLimitKb(Integer memoryLimitKb) { this.memoryLimitKb = memoryLimitKb; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
