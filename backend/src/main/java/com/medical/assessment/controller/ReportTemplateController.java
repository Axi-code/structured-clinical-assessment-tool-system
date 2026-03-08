package com.medical.assessment.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medical.assessment.annotation.OperationLogRecord;
import com.medical.assessment.annotation.RequiresRoles;
import com.medical.assessment.common.PageResult;
import com.medical.assessment.common.Result;
import com.medical.assessment.entity.ReportTemplate;
import com.medical.assessment.service.ReportTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/report-template")
public class ReportTemplateController {
    
    @Autowired
    private ReportTemplateService reportTemplateService;
    
    /**
     * 获取报告模板列表
     */
    @GetMapping("/list")
    @RequiresRoles({"ADMIN", "DOCTOR"})
    public Result<PageResult<ReportTemplate>> getTemplateList(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long assessmentTemplateId,
            @RequestParam(required = false) String templateName,
            @RequestParam(required = false) String reportType) {
        Page<ReportTemplate> page = new Page<>(current, size);
        LambdaQueryWrapper<ReportTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReportTemplate::getDeleted, 0);
        
        if (assessmentTemplateId != null) {
            wrapper.eq(ReportTemplate::getAssessmentTemplateId, assessmentTemplateId);
        }
        if (templateName != null && !templateName.isEmpty()) {
            wrapper.like(ReportTemplate::getTemplateName, templateName);
        }
        if (reportType != null && !reportType.isEmpty()) {
            wrapper.eq(ReportTemplate::getReportType, reportType);
        }
        
        wrapper.orderByDesc(ReportTemplate::getIsDefault);
        wrapper.orderByDesc(ReportTemplate::getCreateTime);
        
        Page<ReportTemplate> result = reportTemplateService.page(page, wrapper);
        return Result.success(PageResult.of(result));
    }
    
    /**
     * 根据评估模板ID获取报告模板列表
     */
    @GetMapping("/assessment-template/{assessmentTemplateId}")
    @RequiresRoles({"ADMIN", "DOCTOR", "NURSE"})
    public Result<List<ReportTemplate>> getByAssessmentTemplateId(@PathVariable Long assessmentTemplateId) {
        List<ReportTemplate> templates = reportTemplateService.getByAssessmentTemplateId(assessmentTemplateId);
        return Result.success(templates);
    }
    
    /**
     * 获取报告模板详情
     */
    @GetMapping("/{id}")
    @RequiresRoles({"ADMIN", "DOCTOR", "NURSE"})
    public Result<ReportTemplate> getTemplate(@PathVariable Long id) {
        ReportTemplate template = reportTemplateService.getById(id);
        return Result.success(template);
    }
    
    /**
     * 创建报告模板
     */
    @PostMapping("/create")
    @RequiresRoles({"ADMIN"})
    @OperationLogRecord(
            module = "REPORT_TEMPLATE",
            action = "CREATE",
            targetType = "REPORT_TEMPLATE",
            targetId = "#template.id",
            description = "'创建报告模板：' + #template.templateName"
    )
    public Result<ReportTemplate> createTemplate(@RequestBody ReportTemplate template) {
        template.setCreateTime(LocalDateTime.now());
        template.setUpdateTime(LocalDateTime.now());
        template.setDeleted(0);
        if (template.getStatus() == null) {
            template.setStatus(1);
        }
        if (template.getIsDefault() == null) {
            template.setIsDefault(0);
        }
        
        reportTemplateService.save(template);
        
        // 如果设置为默认模板，需要取消其他模板的默认状态
        if (template.getIsDefault() == 1) {
            reportTemplateService.setDefaultTemplate(template.getId());
        }
        
        return Result.success(template);
    }
    
    /**
     * 更新报告模板
     */
    @PutMapping("/{id}")
    @RequiresRoles({"ADMIN"})
    @OperationLogRecord(
            module = "REPORT_TEMPLATE",
            action = "UPDATE",
            targetType = "REPORT_TEMPLATE",
            targetId = "#id",
            description = "'更新报告模板ID：' + #id"
    )
    public Result<ReportTemplate> updateTemplate(@PathVariable Long id, @RequestBody ReportTemplate template) {
        template.setId(id);
        template.setUpdateTime(LocalDateTime.now());
        reportTemplateService.updateById(template);
        
        // 如果设置为默认模板，需要取消其他模板的默认状态
        if (template.getIsDefault() == 1) {
            reportTemplateService.setDefaultTemplate(id);
        }
        
        ReportTemplate updated = reportTemplateService.getById(id);
        return Result.success(updated);
    }
    
    /**
     * 删除报告模板
     */
    @DeleteMapping("/{id}")
    @RequiresRoles({"ADMIN"})
    @OperationLogRecord(
            module = "REPORT_TEMPLATE",
            action = "DELETE",
            targetType = "REPORT_TEMPLATE",
            targetId = "#id",
            description = "'删除报告模板ID：' + #id"
    )
    public Result<Void> deleteTemplate(@PathVariable Long id) {
        ReportTemplate template = reportTemplateService.getById(id);
        if (template == null || template.getDeleted() == 1) {
            return Result.error("报告模板不存在");
        }
        
        template.setDeleted(1);
        template.setUpdateTime(LocalDateTime.now());
        reportTemplateService.updateById(template);
        
        return Result.success();
    }
    
    /**
     * 设置默认模板
     */
    @PutMapping("/{id}/set-default")
    @RequiresRoles({"ADMIN"})
    @OperationLogRecord(
            module = "REPORT_TEMPLATE",
            action = "SET_DEFAULT",
            targetType = "REPORT_TEMPLATE",
            targetId = "#id",
            description = "'设置报告模板为默认 ID=' + #id"
    )
    public Result<Void> setDefaultTemplate(@PathVariable Long id) {
        reportTemplateService.setDefaultTemplate(id);
        return Result.success();
    }
    
    /**
     * 更新模板状态
     */
    @PutMapping("/{id}/status")
    @RequiresRoles({"ADMIN"})
    @OperationLogRecord(
            module = "REPORT_TEMPLATE",
            action = "STATUS",
            targetType = "REPORT_TEMPLATE",
            targetId = "#id",
            description = "'更新报告模板状态 ID=' + #id + ' status=' + #status"
    )
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        ReportTemplate template = reportTemplateService.getById(id);
        if (template == null || template.getDeleted() == 1) {
            return Result.error("报告模板不存在");
        }
        
        template.setStatus(status);
        template.setUpdateTime(LocalDateTime.now());
        reportTemplateService.updateById(template);
        
        return Result.success();
    }
}
