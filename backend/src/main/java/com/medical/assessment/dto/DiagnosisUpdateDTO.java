package com.medical.assessment.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

@Data
@EqualsAndHashCode(callSuper = true)
public class DiagnosisUpdateDTO extends DiagnosisCreateDTO {
    @NotNull(message = "诊断ID不能为空")
    private Long id;
}
