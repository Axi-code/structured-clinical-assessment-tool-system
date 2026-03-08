package com.medical.assessment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaVO {
    /** 验证码唯一 key，登录时与 captchaCode 一并提交 */
    private String captchaKey;
    /** 验证码图片 Base64（data:image/png;base64,...） */
    private String captchaImage;
}
