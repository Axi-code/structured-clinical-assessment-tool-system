package com.medical.assessment.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class AssessmentDraftCreateDTO {
    @NotNull(message = "patientId 不能为空")
    private Long patientId;

    @NotNull(message = "templateId 不能为空")
    private Long templateId;
}
