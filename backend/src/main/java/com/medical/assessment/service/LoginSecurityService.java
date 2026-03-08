package com.medical.assessment.service;

/**
 * 登录安全相关：限流、失败计数、验证码校验。
 */
public interface LoginSecurityService {

    /**
     * 检查该 IP 是否超过登录请求限流（如每分钟最多 N 次），超过则抛出异常。
     */
    void checkRateLimit(String clientIp);

    /**
     * 该 IP 是否需要验证码（例如连续失败次数 >= 3）。
     */
    boolean needCaptcha(String clientIp);

    /**
     * 校验验证码是否正确，校验后删除该 key。
     */
    boolean verifyCaptcha(String captchaKey, String captchaCode);

    /**
     * 记录一次登录失败（用于触发“需要验证码”）。
     */
    void recordLoginFail(String clientIp);

    /**
     * 登录成功后清除该 IP 的失败计数。
     */
    void clearLoginFail(String clientIp);

    /**
     * 保存验证码答案，返回 captchaKey（前端提交登录时带上）。
     */
    String saveCaptcha(String code);

    /** 限流：时间窗口（毫秒），默认 1 分钟 */
    long getRateLimitWindowMs();

    /** 限流：窗口内最大请求次数 */
    int getRateLimitMaxPerWindow();
}
