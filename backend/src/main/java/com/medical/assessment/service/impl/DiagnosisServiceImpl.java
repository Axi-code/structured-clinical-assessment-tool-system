package com.medical.assessment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.medical.assessment.dto.DiagnosisCreateDTO;
import com.medical.assessment.dto.DiagnosisUpdateDTO;
import com.medical.assessment.entity.Diagnosis;
import com.medical.assessment.exception.BusinessException;
import com.medical.assessment.mapper.DiagnosisMapper;
import com.medical.assessment.service.DiagnosisService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DiagnosisServiceImpl extends ServiceImpl<DiagnosisMapper, Diagnosis> implements DiagnosisService {

    @Override
    public List<Diagnosis> listByDepartmentId(Long departmentId) {
        if (departmentId == null) {
            return java.util.Collections.emptyList();
        }
        return list(new LambdaQueryWrapper<Diagnosis>()
                .eq(Diagnosis::getDepartmentId, departmentId)
                .eq(Diagnosis::getDeleted, 0)
                .orderByAsc(Diagnosis::getSortOrder));
    }

    @Override
    public Diagnosis createDiagnosis(DiagnosisCreateDTO createDTO) {
        Diagnosis diagnosis = new Diagnosis();
        fillDiagnosis(diagnosis, createDTO);
        diagnosis.setCreateTime(LocalDateTime.now());
        diagnosis.setUpdateTime(LocalDateTime.now());
        diagnosis.setDeleted(0);
        save(diagnosis);
        return diagnosis;
    }

    @Override
    public void updateDiagnosis(DiagnosisUpdateDTO updateDTO) {
        Diagnosis diagnosis = getById(updateDTO.getId());
        if (diagnosis == null) {
            throw new BusinessException("诊断不存在");
        }
        fillDiagnosis(diagnosis, updateDTO);
        diagnosis.setUpdateTime(LocalDateTime.now());
        updateById(diagnosis);
    }

    private void fillDiagnosis(Diagnosis diagnosis, DiagnosisCreateDTO source) {
        diagnosis.setName(source.getName());
        diagnosis.setDepartmentId(source.getDepartmentId());
        diagnosis.setIcdCode(source.getIcdCode());
        diagnosis.setSortOrder(source.getSortOrder() == null ? 0 : source.getSortOrder());
        diagnosis.setRemark(source.getRemark());
    }
}
