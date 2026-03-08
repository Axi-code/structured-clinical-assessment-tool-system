package com.medical.assessment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Qwen API配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "qwen")
public class QwenConfig {
    /**
     * API Key
     */
    private String apiKey;
    
    /**
     * API URL
     */
    private String apiUrl;
    
    /**
     * 模型名称
     */
    private String model = "qwen-turbo";
    
    /**
     * 超时时间（毫秒）
     */
    private Integer timeout = 30000;
}
