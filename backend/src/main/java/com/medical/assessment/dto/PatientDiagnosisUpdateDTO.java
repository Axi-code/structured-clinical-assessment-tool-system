package com.medical.assessment.dto;

import lombok.Data;

/**
 * 患者当前诊断确认/修改请求
 */
@Data
public class PatientDiagnosisUpdateDTO {
    /**
     * 当前确诊诊断ID；为空表示清空当前诊断
     */
    private Long diagnosisId;
}
