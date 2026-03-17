package com.medical.assessment.dto;

/**
 * 用户更新请求 DTO
 * 用途：承接“编辑用户”时提交的更新信息（含用户 ID 与启停用状态等）。
 * 谁传给谁：前端用户管理-编辑用户页面/弹窗 → `UserController.updateUser` → `UserService.updateUser`（`UserServiceImpl.updateUser`）
 */
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
