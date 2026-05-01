package com.structexam.common.dto;

import lombok.Data;

@Data
public class CodeSaveRequest {
    private Long examId;
    private Long questionId;
    private String code;
    private String language;
}
