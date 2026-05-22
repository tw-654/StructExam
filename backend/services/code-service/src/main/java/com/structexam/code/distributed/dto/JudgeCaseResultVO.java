package com.structexam.code.distributed.dto;

/**
 * 单条测试用例判定结果 VO。
 * 当 isPublic=false 且判题未通过时，inputData / expectedOutput / actualOutput 由服务端置 null，
 * 前端仅可见 status 和 caseIndex。
 */
public class JudgeCaseResultVO {

    private Integer caseIndex;
    private String caseName;
    private String status;
    private Boolean passed;
    private Boolean isPublic;

    /** 仅 isPublic=true 或通过时填充 */
    private String inputData;
    private String expectedOutput;
    private String actualOutput;

    private String errorMessage;
    private Long timeUsedMs;
    private Long memoryUsedKb;
    private Integer score;

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

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
}
