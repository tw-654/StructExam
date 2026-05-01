package com.structexam.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_question")
public class Question extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long examId;

    private String type;

    private String title;

    private String content;

    private String options;

    private Integer score;

    private Integer sortOrder;
}
