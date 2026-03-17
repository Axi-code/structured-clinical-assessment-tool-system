package com.medical.assessment.dto;

/**
 * 评估记录保存（草稿） DTO
 * 用途：承接“保存评估草稿”时提交的数据（recordId、当前状态、表单数据）。
 * 谁传给谁：前端评估填写页面（点击保存/暂存）→ `AssessmentRecordController.saveAssessment` →（后端持久化草稿数据）
 */
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.Map;

@Data
public class AssessmentSaveDTO {
    @NotNull(message = "recordId 不能为空")
    private Long recordId;

    private Integer status = 0;

    private Map<String, Object> assessmentData;
}
