package com.structexam.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.structexam.common.entity.Question;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QuestionMapper extends BaseMapper<Question> {
}
