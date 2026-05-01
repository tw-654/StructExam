package com.structexam.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_exam_record")
public class ExamRecord extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long examId;

    private Long userId;

    private LocalDateTime enterTime;

    private LocalDateTime submitTime;

    private Integer score;

    private String status;

    private String ipAddress;
}
