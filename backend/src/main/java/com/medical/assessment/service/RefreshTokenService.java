package com.medical.assessment.service;

/**
 * RefreshToken 的创建、校验、撤销。RefreshToken 存于 httpOnly Cookie，用于换取新的 accessToken。
 */
public interface RefreshTokenService {

    /**
     * 为该用户创建 refreshToken，返回 token 字符串（写入 Cookie）。
     */
    String create(Long userId);

    /**
     * 校验 refreshToken，若有效返回 userId 并撤销该 token（一次性使用后需用新 token 替换）。
     */
    Long validateAndConsume(String refreshToken);

    /**
     * 撤销该用户当前所有 refreshToken（登出时调用）。
     */
    void revokeByUserId(Long userId);
}
