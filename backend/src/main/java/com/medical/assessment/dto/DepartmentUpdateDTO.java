package com.medical.assessment.dto;

/**
 * 科室更新请求 DTO
 * 用途：承接“编辑科室”时提交的更新信息（在新增字段基础上补充科室 ID）。
 * 谁传给谁：前端科室管理-编辑科室页面/弹窗 → `DepartmentController.update` → `DepartmentService.updateDepartment`（`DepartmentServiceImpl.updateDepartment`）
 */
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

@Data
@EqualsAndHashCode(callSuper = true)
public class DepartmentUpdateDTO extends DepartmentCreateDTO {
    @NotNull(message = "科室ID不能为空")
    private Long id;
}
