package com.medical.assessment.dto;

/**
 * 患者当前诊断确认/修改请求 DTO
 * 用途：承接“修改患者当前诊断”时提交的数据（诊断 ID，可为空表示清空）。
 * 谁传给谁：前端患者详情页/诊断确认区域 → `PatientController.updatePatientDiagnosis` →（在该接口内完成更新逻辑）
 */
import lombok.Data;

@Data
public class PatientDiagnosisUpdateDTO {
    /**
     * 当前确诊诊断ID；为空表示清空当前诊断
     */
    private Long diagnosisId;
}
