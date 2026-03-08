package com.medical.assessment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.medical.assessment.entity.Diagnosis;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DiagnosisMapper extends BaseMapper<Diagnosis> {
}
