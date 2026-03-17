package com.medical.assessment.dto;

/**
 * 诊断更新请求 DTO
 * 用途：承接“编辑诊断”时提交的更新信息（在新增字段基础上补充诊断 ID）。
 * 谁传给谁：前端诊断管理-编辑诊断页面/弹窗 → `DiagnosisController.update` → `DiagnosisService.updateDiagnosis`（`DiagnosisServiceImpl.updateDiagnosis`）
 */
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

@Data
@EqualsAndHashCode(callSuper = true)
public class DiagnosisUpdateDTO extends DiagnosisCreateDTO {
    @NotNull(message = "诊断ID不能为空")
    private Long id;
}
