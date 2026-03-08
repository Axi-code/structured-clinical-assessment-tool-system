package com.medical.assessment.config;

import com.medical.assessment.util.AesEncryptionUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * 加密配置：启动时注入 AES 密钥到工具类
 * 密钥应从环境变量或配置中心获取，切勿硬编码
 */
@Configuration
public class EncryptionConfig {

    @Value("${medical.encryption.aes-key:DemoAesKey123456}")
    private String aesKey;

    @PostConstruct
    public void init() {
        AesEncryptionUtil.setKey(aesKey);
    }
}
