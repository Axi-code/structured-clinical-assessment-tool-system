package com.medical.assessment.dto;

/**
 * 评估模板更新请求 DTO
 * 用途：承接“编辑评估模板”时提交的更新信息（在新增字段基础上补充模板 ID）。
 * 谁传给谁：前端模板管理-编辑模板页面 → `AssessmentTemplateController.updateTemplate` → `AssessmentTemplateService.updateTemplate`（`AssessmentTemplateServiceImpl.updateTemplate`）
 */
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

@Data
@EqualsAndHashCode(callSuper = true)
public class AssessmentTemplateUpdateDTO extends AssessmentTemplateCreateDTO {
    @NotNull(message = "模板ID不能为空")
    private Long id;
}
