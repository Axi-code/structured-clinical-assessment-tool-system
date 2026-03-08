package com.medical.assessment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码编码器配置。
 * 使用 BCrypt 进行密码哈希，不引入 Spring Security 完整框架。
 */
@Configuration
public class PasswordEncoderConfig {

    /**
     * BCrypt 强度（log rounds），默认 10，约 2^10 次迭代。
     * 值越大越安全但越慢，10 为常用折中。
     */
    private static final int BCRYPT_STRENGTH = 10;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(BCRYPT_STRENGTH);
    }
}
