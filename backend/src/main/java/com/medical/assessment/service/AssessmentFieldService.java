package com.medical.assessment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.medical.assessment.entity.AssessmentField;

import java.util.List;

public interface AssessmentFieldService extends IService<AssessmentField> {
    List<AssessmentField> getFieldsByTemplateId(Long templateId);
}

