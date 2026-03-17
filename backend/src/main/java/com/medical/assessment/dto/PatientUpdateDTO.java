package com.medical.assessment.dto;

/**
 * 患者信息更新请求 DTO
 * 用途：承接“编辑患者信息”时提交的数据（在新增字段基础上补充患者 ID）。
 * 谁传给谁：前端患者管理-编辑患者页面 → `PatientController.updatePatient` → `PatientService.updatePatient`（`PatientServiceImpl.updatePatient`）
 */
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

@Data
@EqualsAndHashCode(callSuper = true)
public class PatientUpdateDTO extends PatientCreateDTO {
    @NotNull(message = "患者ID不能为空")
    private Long id;
}
