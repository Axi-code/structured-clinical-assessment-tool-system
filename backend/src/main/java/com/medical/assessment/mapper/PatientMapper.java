package com.medical.assessment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.medical.assessment.entity.Patient;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PatientMapper extends BaseMapper<Patient> {
}

