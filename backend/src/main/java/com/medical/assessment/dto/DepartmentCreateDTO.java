package com.medical.assessment.dto;

/**
 * 科室新增请求 DTO
 * 用途：承接“新增科室”时提交的科室信息（名称、编码、排序、备注）。
 * 谁传给谁：前端科室管理-新增科室页面/弹窗 → `DepartmentController.add` → `DepartmentService.createDepartment`（`DepartmentServiceImpl.createDepartment`）
 */
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class DepartmentCreateDTO {
    @NotBlank(message = "科室名称不能为空")
    @Size(max = 50, message = "科室名称长度不能超过50个字符")
    private String name;

    @NotBlank(message = "科室编码不能为空")
    @Size(max = 50, message = "科室编码长度不能超过50个字符")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "科室编码只能包含字母、数字、下划线或短横线")
    private String code;

    private Integer sortOrder;

    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;
}
