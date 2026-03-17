package com.medical.assessment.dto;

/**
 * 评估记录草稿创建 DTO
 * 用途：承接“开始一次评估/创建草稿”时提交的数据（患者 ID、模板 ID）。
 * 谁传给谁：前端评估填写页面（选择患者+模板后）→ `AssessmentRecordController.createDraft` →（后端创建评估记录草稿）
 */
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class AssessmentDraftCreateDTO {
    @NotNull(message = "patientId 不能为空")
    private Long patientId;

    @NotNull(message = "templateId 不能为空")
    private Long templateId;
}
