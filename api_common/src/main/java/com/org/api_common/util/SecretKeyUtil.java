package com.org.api_common.util;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * 密钥生成工具类（注释：生成安全的随机secret_key）
 */
public class SecretKeyUtil {
    // 密钥长度（32字节 → Base64编码后约43位，足够安全）
    private static final int KEY_LENGTH = 32;
    // 安全随机数生成器（避免伪随机）
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * 生成随机secret_key（Base64编码，便于存储/传输）
     */
    public static String generateRandomSecretKey() {
        byte[] keyBytes = new byte[KEY_LENGTH];
        SECURE_RANDOM.nextBytes(keyBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(keyBytes);
    }
}