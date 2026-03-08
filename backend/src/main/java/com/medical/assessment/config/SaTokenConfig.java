package com.medical.assessment.config;

import org.springframework.context.annotation.Configuration;

/**
 * Sa-Token配置类
 * Sa-Token的基础配置通过application.yml完成
 * Token获取逻辑在SaTokenInterceptor中处理（从Authorization header读取Bearer token）
 */
@Configuration
public class SaTokenConfig {
    // Sa-Token配置通过application.yml完成
    // Token获取逻辑在SaTokenInterceptor中处理
    // 拦截器会从Authorization: Bearer {token} header中提取token并设置到Sa-Token的存储中
}
