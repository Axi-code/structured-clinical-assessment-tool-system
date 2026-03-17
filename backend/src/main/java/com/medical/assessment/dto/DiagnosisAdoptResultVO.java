package com.medical.assessment.dto;

/**
 * 采纳 AI 最新诊断结果 VO
 * 用途：后端返回“采纳 AI 诊断”后的结果（最终诊断、AI 给的诊断名、是否新建等）。
 * 谁传给哪个页面：后端采纳 AI 诊断接口 → 前端患者详情页（展示采纳结果/提示）
 */
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
