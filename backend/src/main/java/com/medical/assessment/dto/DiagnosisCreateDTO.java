package com.medical.assessment.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class DiagnosisCreateDTO {
    @NotBlank(message = "诊断名称不能为空")
    @Size(max = 100, message = "诊断名称长度不能超过100个字符")
    private String name;

    @NotNull(message = "所属科室不能为空")
    private Long departmentId;

    @Size(max = 50, message = "ICD编码长度不能超过50个字符")
    @Pattern(regexp = "^[A-Za-z0-9.\\-_]*$", message = "ICD编码格式不正确")
    private String icdCode;

    private Integer sortOrder;

    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;
}
