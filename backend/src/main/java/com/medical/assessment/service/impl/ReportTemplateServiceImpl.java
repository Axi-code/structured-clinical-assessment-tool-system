package com.medical.assessment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.medical.assessment.entity.ReportTemplate;
import com.medical.assessment.mapper.ReportTemplateMapper;
import com.medical.assessment.service.ReportTemplateService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReportTemplateServiceImpl extends ServiceImpl<ReportTemplateMapper, ReportTemplate> implements ReportTemplateService {
    
    @Override
    public List<ReportTemplate> getByAssessmentTemplateId(Long assessmentTemplateId) {
        LambdaQueryWrapper<ReportTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReportTemplate::getAssessmentTemplateId, assessmentTemplateId);
        wrapper.eq(ReportTemplate::getDeleted, 0);
        wrapper.eq(ReportTemplate::getStatus, 1);
        wrapper.orderByDesc(ReportTemplate::getIsDefault);
        wrapper.orderByDesc(ReportTemplate::getCreateTime);
        return list(wrapper);
    }
    
    @Override
    public ReportTemplate getDefaultTemplate(Long assessmentTemplateId, String reportType) {
        LambdaQueryWrapper<ReportTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReportTemplate::getAssessmentTemplateId, assessmentTemplateId);
        wrapper.eq(ReportTemplate::getReportType, reportType);
        wrapper.eq(ReportTemplate::getDeleted, 0);
        wrapper.eq(ReportTemplate::getStatus, 1);
        wrapper.eq(ReportTemplate::getIsDefault, 1);
        wrapper.last("LIMIT 1");
        ReportTemplate template = getOne(wrapper);
        
        // 如果没有默认模板，获取第一个启用的模板
        if (template == null) {
            wrapper.clear();
            wrapper.eq(ReportTemplate::getAssessmentTemplateId, assessmentTemplateId);
            wrapper.eq(ReportTemplate::getReportType, reportType);
            wrapper.eq(ReportTemplate::getDeleted, 0);
            wrapper.eq(ReportTemplate::getStatus, 1);
            wrapper.last("LIMIT 1");
            template = getOne(wrapper);
        }
        
        return template;
    }
    
    @Override
    public void setDefaultTemplate(Long templateId) {
        ReportTemplate template = getById(templateId);
        if (template == null) {
            throw new RuntimeException("报告模板不存在");
        }
        
        // 取消同类型模板的默认状态
        LambdaQueryWrapper<ReportTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReportTemplate::getAssessmentTemplateId, template.getAssessmentTemplateId());
        wrapper.eq(ReportTemplate::getReportType, template.getReportType());
        wrapper.eq(ReportTemplate::getDeleted, 0);
        wrapper.ne(ReportTemplate::getId, templateId);
        
        List<ReportTemplate> templates = list(wrapper);
        for (ReportTemplate t : templates) {
            t.setIsDefault(0);
            updateById(t);
        }
        
        // 设置当前模板为默认
        template.setIsDefault(1);
        updateById(template);
    }
}
