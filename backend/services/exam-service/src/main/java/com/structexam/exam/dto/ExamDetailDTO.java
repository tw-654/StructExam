package com.structexam.exam.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ExamDetailDTO {
    private Long id;
    private String title;
    private String description;
    private Integer duration;
    private Integer totalScore;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private List<QuestionDTO> questions;
}
