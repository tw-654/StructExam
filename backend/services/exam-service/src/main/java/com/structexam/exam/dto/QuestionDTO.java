package com.structexam.exam.dto;

import lombok.Data;

@Data
public class QuestionDTO {
    private Long id;
    private String type;
    private String title;
    private String content;
    private String options;
    private Integer score;
    private Integer sortOrder;
}
