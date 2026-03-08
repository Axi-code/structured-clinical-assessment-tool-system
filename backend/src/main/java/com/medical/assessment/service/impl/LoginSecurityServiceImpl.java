package com.medical.assessment.service.impl;

import com.medical.assessment.service.LoginSecurityService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录安全：按 IP 限流、失败次数记录、验证码存储。使用内存存储，单机有效。
 */
@Service
public class LoginSecurityServiceImpl implements LoginSecurityService {

    private static final long RATE_LIMIT_WINDOW_MS = 60_000L;   // 1 分钟
    private static final int RATE_LIMIT_MAX_PER_WINDOW = 10;     // 每分钟最多 10 次登录请求
    private static final long FAIL_COUNT_WINDOW_MS = 15 * 60_000L; // 15 分钟内失败次数
    private static final int FAIL_COUNT_THRESHOLD = 3;          // 失败 3 次后需验证码
    private static final long CAPTCHA_TTL_MS = 5 * 60_000L;     // 验证码 5 分钟有效

    @Value("${login.security.rate-limit-window-ms:" + RATE_LIMIT_WINDOW_MS + "}")
    private long rateLimitWindowMs;

    @Value("${login.security.rate-limit-max:" + RATE_LIMIT_MAX_PER_WINDOW + "}")
    private int rateLimitMaxPerWindow;

    private final Map<String, RateLimitEntry> rateLimitMap = new ConcurrentHashMap<>();
    private final Map<String, FailEntry> failCountMap = new ConcurrentHashMap<>();
    private final Map<String, CaptchaEntry> captchaStore = new ConcurrentHashMap<>();

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
        }, "login-security-cleanup");
        t.setDaemon(true);
        t.start();
    }

    private void cleanupExpired() {
        long now = System.currentTimeMillis();
        rateLimitMap.entrySet().removeIf(e -> now - e.getValue().windowStartMs > rateLimitWindowMs);
        failCountMap.entrySet().removeIf(e -> now - e.getValue().windowStartMs > FAIL_COUNT_WINDOW_MS);
        captchaStore.entrySet().removeIf(e -> now > e.getValue().expireAtMs);
    }

    @Override
    public void checkRateLimit(String clientIp) {
        long now = System.currentTimeMillis();
        RateLimitEntry entry = rateLimitMap.compute(clientIp, (k, v) -> {
            if (v == null || now - v.windowStartMs > rateLimitWindowMs) {
                return new RateLimitEntry(1, now);
            }
            v.count++;
            return v;
        });
        if (entry.count > rateLimitMaxPerWindow) {
            throw new RuntimeException("请求过于频繁，请稍后再试");
        }
    }

    @Override
    public boolean needCaptcha(String clientIp) {
        FailEntry entry = failCountMap.get(clientIp);
        if (entry == null) return false;
        if (System.currentTimeMillis() - entry.windowStartMs > FAIL_COUNT_WINDOW_MS) {
            failCountMap.remove(clientIp);
            return false;
        }
        return entry.count >= FAIL_COUNT_THRESHOLD;
    }

    @Override
    public boolean verifyCaptcha(String captchaKey, String captchaCode) {
        if (captchaKey == null || captchaKey.isEmpty() || captchaCode == null || captchaCode.isEmpty()) {
            return false;
        }
        CaptchaEntry entry = captchaStore.remove(captchaKey);
        if (entry == null || System.currentTimeMillis() > entry.expireAtMs) {
            return false;
        }
        return entry.code.equalsIgnoreCase(captchaCode.trim());
    }

    @Override
    public void recordLoginFail(String clientIp) {
        long now = System.currentTimeMillis();
        failCountMap.compute(clientIp, (k, v) -> {
            if (v == null || now - v.windowStartMs > FAIL_COUNT_WINDOW_MS) {
                return new FailEntry(1, now);
            }
            v.count++;
            return v;
        });
    }

    @Override
    public void clearLoginFail(String clientIp) {
        failCountMap.remove(clientIp);
    }

    @Override
    public String saveCaptcha(String code) {
        String key = UUID.randomUUID().toString();
        captchaStore.put(key, new CaptchaEntry(code, System.currentTimeMillis() + CAPTCHA_TTL_MS));
        return key;
    }

    @Override
    public long getRateLimitWindowMs() {
        return rateLimitWindowMs;
    }

    @Override
    public int getRateLimitMaxPerWindow() {
        return rateLimitMaxPerWindow;
    }

    private static class RateLimitEntry {
        int count;
        final long windowStartMs;

        RateLimitEntry(int count, long windowStartMs) {
            this.count = count;
            this.windowStartMs = windowStartMs;
        }
    }

    private static class FailEntry {
        int count;
        final long windowStartMs;

        FailEntry(int count, long windowStartMs) {
            this.count = count;
            this.windowStartMs = windowStartMs;
        }
    }

    private static class CaptchaEntry {
        final String code;
        final long expireAtMs;

        CaptchaEntry(String code, long expireAtMs) {
            this.code = code;
            this.expireAtMs = expireAtMs;
        }
    }
}
