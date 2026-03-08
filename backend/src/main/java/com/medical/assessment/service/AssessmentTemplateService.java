package com.medical.assessment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.medical.assessment.dto.AssessmentTemplateCreateDTO;
import com.medical.assessment.dto.AssessmentTemplateUpdateDTO;
import com.medical.assessment.dto.AssessmentTemplateVersionCreateDTO;
import com.medical.assessment.entity.AssessmentTemplate;

public interface AssessmentTemplateService extends IService<AssessmentTemplate> {
    AssessmentTemplate getTemplateDetail(Long id);
    AssessmentTemplate createTemplate(AssessmentTemplateCreateDTO createDTO);
    void updateTemplate(AssessmentTemplateUpdateDTO updateDTO);
    void updateTemplateStatus(Long id, Integer status);
    AssessmentTemplate createNewVersion(Long id, AssessmentTemplateVersionCreateDTO newVersionDTO);
}

