package com.structexam.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_exam")
public class Exam extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String description;

    private Integer duration;

    private Integer totalScore;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String status;

    private Long creatorId;
}
