package com.medical.assessment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.medical.assessment.entity.ReportTemplate;

import java.util.List;

public interface ReportTemplateService extends IService<ReportTemplate> {
    /**
     * 根据评估模板ID获取报告模板列表
     */
    List<ReportTemplate> getByAssessmentTemplateId(Long assessmentTemplateId);
    
    /**
     * 获取默认报告模板
     */
    ReportTemplate getDefaultTemplate(Long assessmentTemplateId, String reportType);
    
    /**
     * 设置默认模板
     */
    void setDefaultTemplate(Long templateId);
}
