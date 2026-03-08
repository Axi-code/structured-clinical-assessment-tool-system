package com.medical.assessment.controller;

import com.medical.assessment.annotation.OperationLogRecord;
import com.medical.assessment.annotation.RequiresRoles;
import com.medical.assessment.common.Result;
import com.medical.assessment.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;

/**
 * 评估报告生成控制器
 */
@RestController
@RequestMapping("/report")
public class ReportController {
    
    @Autowired
    private ReportService reportService;
    
    /**
     * 预览PDF报告（返回Base64编码）
     */
    @GetMapping("/preview/pdf/{recordId}")
    @RequiresRoles({"ADMIN", "DOCTOR", "NURSE"})
    @OperationLogRecord(
            module = "REPORT",
            action = "PREVIEW_PDF",
            targetType = "ASSESSMENT_RECORD",
            targetId = "#recordId",
            description = "'预览PDF报告 recordId=' + #recordId + ( #templateId != null ? ' templateId=' + #templateId : '' )"
    )
    public Result<String> previewPdfReport(
            @PathVariable Long recordId,
            @RequestParam(required = false) Long templateId) {
        return Result.success(reportService.previewPdfReport(recordId, templateId));
    }
    
    /**
     * 预览Word报告（返回Base64编码）
     */
    @GetMapping("/preview/word/{recordId}")
    @RequiresRoles({"ADMIN", "DOCTOR", "NURSE"})
    @OperationLogRecord(
            module = "REPORT",
            action = "PREVIEW_WORD",
            targetType = "ASSESSMENT_RECORD",
            targetId = "#recordId",
            description = "'预览Word报告 recordId=' + #recordId + ( #templateId != null ? ' templateId=' + #templateId : '' )"
    )
    public Result<String> previewWordReport(
            @PathVariable Long recordId,
            @RequestParam(required = false) Long templateId) {
        return Result.success(reportService.previewWordReport(recordId, templateId));
    }
    
    /**
     * 生成PDF报告并下载（使用自定义模板）
     */
    @GetMapping("/pdf/{recordId}")
    @RequiresRoles({"ADMIN", "DOCTOR", "NURSE"})
    @OperationLogRecord(
            module = "REPORT",
            action = "GENERATE_PDF",
            targetType = "ASSESSMENT_RECORD",
            targetId = "#recordId",
            description = "'生成PDF报告 recordId=' + #recordId + ( #templateId != null ? ' templateId=' + #templateId : '' )"
    )
    public void generatePdfReport(
            @PathVariable Long recordId,
            @RequestParam(required = false) Long templateId,
            HttpServletResponse response) {
        reportService.generatePdfReport(recordId, templateId, response);
    }
    
    /**
     * 生成Word报告并下载（使用自定义模板）
     */
    @GetMapping("/word/{recordId}")
    @RequiresRoles({"ADMIN", "DOCTOR", "NURSE"})
    @OperationLogRecord(
            module = "REPORT",
            action = "GENERATE_WORD",
            targetType = "ASSESSMENT_RECORD",
            targetId = "#recordId",
            description = "'生成Word报告 recordId=' + #recordId + ( #templateId != null ? ' templateId=' + #templateId : '' )"
    )
    public void generateWordReport(
            @PathVariable Long recordId,
            @RequestParam(required = false) Long templateId,
            HttpServletResponse response) {
        reportService.generateWordReport(recordId, templateId, response);
    }
}
