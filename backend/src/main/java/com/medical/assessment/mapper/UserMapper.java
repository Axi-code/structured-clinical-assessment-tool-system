package com.medical.assessment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.medical.assessment.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}

