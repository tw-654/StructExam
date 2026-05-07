package com.structexam.common.dto;

import lombok.Data;

import java.util.List;

@Data
public class CodeExecuteRequest {
    private String code;
    private String language = "java";
    private List<TestCase> testCases;
    private Long timeout = 10L;
}
