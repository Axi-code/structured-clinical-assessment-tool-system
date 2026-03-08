package com.medical.assessment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.medical.assessment.entity.AssessmentField;
import com.medical.assessment.mapper.AssessmentFieldMapper;
import com.medical.assessment.service.AssessmentFieldService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AssessmentFieldServiceImpl extends ServiceImpl<AssessmentFieldMapper, AssessmentField> implements AssessmentFieldService {
    
    @Override
    public List<AssessmentField> getFieldsByTemplateId(Long templateId) {
        return this.list(new LambdaQueryWrapper<AssessmentField>()
                .eq(AssessmentField::getTemplateId, templateId)
                .eq(AssessmentField::getDeleted, 0)
                .orderByAsc(AssessmentField::getSortOrder));
    }
}

