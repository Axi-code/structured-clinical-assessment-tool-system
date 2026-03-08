package com.medical.assessment.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserUpdateDTO extends UserCreateDTO {
    @NotNull(message = "用户ID不能为空")
    private Long id;

    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态只能为0或1")
    @Max(value = 1, message = "状态只能为0或1")
    private Integer status;
}
