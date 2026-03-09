package com.medical.assessment.dto;

import com.medical.assessment.entity.Diagnosis;
import lombok.Data;

@Data
public class DiagnosisAdoptResultVO {
    private Long diagnosisId;
    private String diagnosisName;
    private String aiDiagnosisName;
    private Boolean created;

    public static DiagnosisAdoptResultVO of(Diagnosis diagnosis, String aiDiagnosisName, boolean created) {
        DiagnosisAdoptResultVO vo = new DiagnosisAdoptResultVO();
        vo.setDiagnosisId(diagnosis != null ? diagnosis.getId() : null);
        vo.setDiagnosisName(diagnosis != null ? diagnosis.getName() : null);
        vo.setAiDiagnosisName(aiDiagnosisName);
        vo.setCreated(created);
        return vo;
    }
}
