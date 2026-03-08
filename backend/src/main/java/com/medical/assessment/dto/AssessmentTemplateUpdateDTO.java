package com.medical.assessment.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

@Data
@EqualsAndHashCode(callSuper = true)
public class AssessmentTemplateUpdateDTO extends AssessmentTemplateCreateDTO {
    @NotNull(message = "模板ID不能为空")
    private Long id;
}
