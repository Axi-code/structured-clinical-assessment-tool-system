package com.medical.assessment.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

@Data
@EqualsAndHashCode(callSuper = true)
public class PatientUpdateDTO extends PatientCreateDTO {
    @NotNull(message = "患者ID不能为空")
    private Long id;
}
