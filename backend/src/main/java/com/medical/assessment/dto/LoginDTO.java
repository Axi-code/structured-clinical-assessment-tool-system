package com.medical.assessment.dto;

/**
 * 登录请求参数 DTO
 * 用途：承接“登录页面”表单提交的数据（用户名、密码、验证码）。
 * 谁传给谁：前端登录页面 → `UserController.login` → `UserService.login`（`UserServiceImpl.login`）
 */
import lombok.Data;
import javax.validation.constraints.NotBlank;

@Data
public class LoginDTO {
    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    /** 验证码 key（需验证码时必填） */
    private String captchaKey;

    /** 验证码内容（需验证码时必填） */
    private String captchaCode;
}

