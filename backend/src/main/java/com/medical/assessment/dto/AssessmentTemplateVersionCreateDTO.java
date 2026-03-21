package com.medical.assessment.dto;

/**
 * 评估模板新版本创建 DTO
 * 用途：承接“基于现有模板创建新版本”时提交的新版本信息（名称/类别/描述/状态/备注）。
 * 谁传给谁：前端模板管理-创建新版本操作 → `AssessmentTemplateController.createNewVersion` → `AssessmentTemplateService.createNewVersion`（`AssessmentTemplateServiceImpl.createNewVersion`）
 */
import lombok.Data;

@Data
public class AssessmentTemplateVersionCreateDTO {
    private String templateName;
    private String description;
    private Integer status;
    private String remark;
}
