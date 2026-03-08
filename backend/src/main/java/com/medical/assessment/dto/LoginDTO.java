package com.medical.assessment.dto;

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

