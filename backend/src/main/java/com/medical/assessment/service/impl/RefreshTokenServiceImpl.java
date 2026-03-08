package com.medical.assessment.service.impl;

import com.medical.assessment.service.RefreshTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * RefreshToken Redis 存储：支持多实例、重启不失效。
 */
@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private static final String KEY_TOKEN_PREFIX = "refresh_token:";
    private static final String KEY_USER_PREFIX = "refresh_token:user:";

    @Value("${auth.refresh-token.max-age-seconds:604800}")
    private long maxAgeSeconds;

    private final StringRedisTemplate redisTemplate;

    public RefreshTokenServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public String create(Long userId) {
        if (userId == null) return null;
        String userKey = KEY_USER_PREFIX + userId;
        String oldTokenId = redisTemplate.opsForValue().get(userKey);
        if (oldTokenId != null) {
            redisTemplate.delete(KEY_TOKEN_PREFIX + oldTokenId);
            redisTemplate.delete(userKey);
        }

        String tokenId = UUID.randomUUID().toString();
        String tokenKey = KEY_TOKEN_PREFIX + tokenId;
        long ttlSeconds = maxAgeSeconds;

        redisTemplate.opsForValue().set(tokenKey, String.valueOf(userId), ttlSeconds, TimeUnit.SECONDS);
        redisTemplate.opsForValue().set(userKey, tokenId, ttlSeconds, TimeUnit.SECONDS);
        return tokenId;
    }

    @Override
    public Long validateAndConsume(String refreshToken) {
        if (refreshToken == null || refreshToken.isEmpty()) return null;
        String tokenKey = KEY_TOKEN_PREFIX + refreshToken;
        String userIdStr = redisTemplate.opsForValue().get(tokenKey);
        if (userIdStr == null) return null;

        Long userId = Long.parseLong(userIdStr);
        String userKey = KEY_USER_PREFIX + userId;
        redisTemplate.delete(tokenKey);
        redisTemplate.delete(userKey);
        return userId;
    }

    @Override
    public void revokeByUserId(Long userId) {
        if (userId == null) return;
        String userKey = KEY_USER_PREFIX + userId;
        String tokenId = redisTemplate.opsForValue().get(userKey);
        if (tokenId != null) {
            redisTemplate.delete(KEY_TOKEN_PREFIX + tokenId);
            redisTemplate.delete(userKey);
        }
    }
}
