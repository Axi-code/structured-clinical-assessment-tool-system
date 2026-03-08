package com.medical.assessment.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

@Data
@EqualsAndHashCode(callSuper = true)
public class DepartmentUpdateDTO extends DepartmentCreateDTO {
    @NotNull(message = "科室ID不能为空")
    private Long id;
}
