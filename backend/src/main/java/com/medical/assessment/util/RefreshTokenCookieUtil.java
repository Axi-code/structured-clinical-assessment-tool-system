package com.medical.assessment.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 * 将 refreshToken 写入 httpOnly Cookie 或从请求中读取、清除。
 */
@Component
public class RefreshTokenCookieUtil {

    @Value("${auth.refresh-token.cookie-name:refreshToken}")
    private String cookieName;

    @Value("${auth.refresh-token.max-age-seconds:604800}")
    private long maxAgeSeconds;

    private static final String COOKIE_PATH = "/";
    private static final String SAME_SITE = "SameSite=Lax";

    public void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        if (refreshToken == null) return;
        try {
            String value = URLEncoder.encode(refreshToken, "UTF-8");
            String cookie = cookieName + "=" + value
                    + "; Path=" + COOKIE_PATH
                    + "; HttpOnly"
                    + "; Max-Age=" + maxAgeSeconds
                    + "; " + SAME_SITE;
            response.addHeader("Set-Cookie", cookie);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 not supported", e);
        }
    }

    public void clearRefreshTokenCookie(HttpServletResponse response) {
        String cookie = cookieName + "=; Path=" + COOKIE_PATH + "; HttpOnly; Max-Age=0; " + SAME_SITE;
        response.addHeader("Set-Cookie", cookie);
    }

    public String getRefreshTokenFromCookie(HttpServletRequest request) {
        javax.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (javax.servlet.http.Cookie c : cookies) {
            if (cookieName.equals(c.getName())) {
                String v = c.getValue();
                if (v == null || v.isEmpty()) return null;
                try {
                    return URLDecoder.decode(v, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException("UTF-8 not supported", e);
                }
            }
        }
        return null;
    }
}
