package com.structexam.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.structexam.common.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
