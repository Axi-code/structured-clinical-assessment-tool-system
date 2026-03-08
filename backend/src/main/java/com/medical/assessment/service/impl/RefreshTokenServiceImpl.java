package com.medical.assessment.service.impl;

import com.medical.assessment.service.RefreshTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RefreshToken 内存存储：单机有效，重启后失效。生产可改为 Redis。
 */
@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private static final long DEFAULT_TTL_MS = 7 * 24 * 60 * 60 * 1000L; // 7 天

    @Value("${auth.refresh-token.max-age-seconds:604800}")
    private long maxAgeSeconds;

    /** tokenId -> (userId, expireAtMs) */
    private final Map<String, TokenEntry> tokenStore = new ConcurrentHashMap<>();
    /** userId -> tokenId，保证同一用户仅保留一个有效 refreshToken */
    private final Map<Long, String> userIdToToken = new ConcurrentHashMap<>();

    @PostConstruct
    public void startCleanup() {
        Thread t = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(60_000L);
                    cleanupExpired();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "refresh-token-cleanup");
        t.setDaemon(true);
        t.start();
    }

    private void cleanupExpired() {
        long now = System.currentTimeMillis();
        tokenStore.entrySet().removeIf(e -> now > e.getValue().expireAtMs);
        userIdToToken.entrySet().removeIf(e -> !tokenStore.containsKey(e.getValue()));
    }

    @Override
    public String create(Long userId) {
        if (userId == null) return null;
        String oldId = userIdToToken.remove(userId);
        if (oldId != null) tokenStore.remove(oldId);

        String tokenId = UUID.randomUUID().toString();
        long expireAt = System.currentTimeMillis() + (maxAgeSeconds * 1000L);
        tokenStore.put(tokenId, new TokenEntry(userId, expireAt));
        userIdToToken.put(userId, tokenId);
        return tokenId;
    }

    @Override
    public Long validateAndConsume(String refreshToken) {
        if (refreshToken == null || refreshToken.isEmpty()) return null;
        TokenEntry entry = tokenStore.remove(refreshToken);
        if (entry == null) return null;
        if (System.currentTimeMillis() > entry.expireAtMs) return null;
        userIdToToken.remove(entry.userId);
        return entry.userId;
    }

    @Override
    public void revokeByUserId(Long userId) {
        if (userId == null) return;
        String tokenId = userIdToToken.remove(userId);
        if (tokenId != null) tokenStore.remove(tokenId);
    }

    private static class TokenEntry {
        final long userId;
        final long expireAtMs;

        TokenEntry(long userId, long expireAtMs) {
            this.userId = userId;
            this.expireAtMs = expireAtMs;
        }
    }
}
