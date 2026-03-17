package com.medical.assessment.dto;

/**
 * 评估记录提交 DTO
 * 用途：承接“提交评估”时提交的数据（recordId + 最终表单数据）。
 * 谁传给谁：前端评估填写页面（点击提交）→ `AssessmentRecordController.submitAssessment` →（后端校验并将评估记录置为已提交）
 */
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.Map;

@Data
public class AssessmentSubmitDTO {
    @NotNull(message = "recordId 不能为空")
    private Long recordId;

    private Map<String, Object> assessmentData;
}
