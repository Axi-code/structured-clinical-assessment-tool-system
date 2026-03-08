package com.medical.assessment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.medical.assessment.dto.DiagnosisCreateDTO;
import com.medical.assessment.dto.DiagnosisUpdateDTO;
import com.medical.assessment.entity.Diagnosis;

import java.util.List;

public interface DiagnosisService extends IService<Diagnosis> {
    List<Diagnosis> listByDepartmentId(Long departmentId);
    Diagnosis createDiagnosis(DiagnosisCreateDTO createDTO);
    void updateDiagnosis(DiagnosisUpdateDTO updateDTO);
}
