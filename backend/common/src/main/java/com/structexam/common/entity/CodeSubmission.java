package com.structexam.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_code_submission")
public class CodeSubmission extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long examId;

    private Long userId;

    private Long questionId;

    private String codeContent;

    private String language;

    private String status;

    private LocalDateTime submitTime;
}
