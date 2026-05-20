package com.structexam.code.distributed.dto;

public class LoadTestRequest {
    private int users = 50;
    private int submissionsPerUser = 1;
    private Long examId = 880001L;
    private Long questionIdStart = 880001L;
    private String language = "python";
    private String code = "print(input())";
    private String input = "hello";
    private String expectedOutput = "hello";

    public int getUsers() {
        return users;
    }

    public void setUsers(int users) {
        this.users = users;
    }

    public int getSubmissionsPerUser() {
        return submissionsPerUser;
    }

    public void setSubmissionsPerUser(int submissionsPerUser) {
        this.submissionsPerUser = submissionsPerUser;
    }

    public Long getExamId() {
        return examId;
    }

    public void setExamId(Long examId) {
        this.examId = examId;
    }

    public Long getQuestionIdStart() {
        return questionIdStart;
    }

    public void setQuestionIdStart(Long questionIdStart) {
        this.questionIdStart = questionIdStart;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public String getExpectedOutput() {
        return expectedOutput;
    }

    public void setExpectedOutput(String expectedOutput) {
        this.expectedOutput = expectedOutput;
    }
}
