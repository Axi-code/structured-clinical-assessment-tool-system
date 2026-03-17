package com.medical.assessment.dto;

/**
 * 登录验证码返回 VO
 * 用途：返回验证码 key 与 Base64 图片，给前端展示并在登录时回传校验。
 * 谁传给哪个页面：后端获取验证码接口 → 前端登录页面（展示验证码图片）
 */
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
