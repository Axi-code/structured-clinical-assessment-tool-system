package com.medical.assessment.util;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * AES 加密工具类，用于 L3 级敏感字段落库加密
 * 使用 AES-128-ECB，密钥需 16 字节
 */
public final class AesEncryptionUtil {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";
    private static final int KEY_LENGTH = 16;

    private static volatile byte[] keyBytes;

    private AesEncryptionUtil() {
    }

    /**
     * 设置加密密钥（由 EncryptionConfig 在启动时调用）
     * 密钥长度需为 16 字节（128 位），不足则补齐，超出则截断
     */
    public static void setKey(String key) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("AES key cannot be null or empty");
        }
        byte[] raw = key.getBytes(StandardCharsets.UTF_8);
        if (raw.length < KEY_LENGTH) {
            byte[] padded = new byte[KEY_LENGTH];
            System.arraycopy(raw, 0, padded, 0, raw.length);
            keyBytes = padded;
        } else if (raw.length > KEY_LENGTH) {
            keyBytes = new byte[KEY_LENGTH];
            System.arraycopy(raw, 0, keyBytes, 0, KEY_LENGTH);
        } else {
            keyBytes = raw;
        }
    }

    /**
     * 加密：明文 -> Base64 密文
     */
    public static String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }
        if (keyBytes == null) {
            throw new IllegalStateException("AES key not initialized. Check EncryptionConfig.");
        }
        try {
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, ALGORITHM);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("AES encrypt failed", e);
        }
    }

    /**
     * 解密：Base64 密文 -> 明文
     * 若解密失败（如旧数据为明文），返回原值以兼容迁移
     */
    public static String decrypt(String ciphertext) {
        if (ciphertext == null || ciphertext.isEmpty()) {
            return ciphertext;
        }
        if (keyBytes == null) {
            throw new IllegalStateException("AES key not initialized. Check EncryptionConfig.");
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(ciphertext);
            if (decoded == null || decoded.length == 0) {
                return ciphertext;
            }
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, ALGORITHM);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decrypted = cipher.doFinal(decoded);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // 兼容旧数据：若解密失败，视为明文（迁移前数据）
            return ciphertext;
        }
    }
}
