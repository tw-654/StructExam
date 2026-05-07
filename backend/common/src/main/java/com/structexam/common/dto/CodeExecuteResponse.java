package com.structexam.common.dto;

import lombok.Data;

import java.util.List;

@Data
public class CodeExecuteResponse {
    private boolean success;
    private String message;
    private List<TestResult> testResults;
    private String compileError;
    private String runtimeError;
    private Long executionTime;

    @Data
    public static class TestResult {
        private boolean passed;
        private String input;
        private String expectedOutput;
        private String actualOutput;
        private String description;
        private Long executionTime;
    }
}
