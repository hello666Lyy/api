package com.org.api_sdk.util;

import java.util.UUID;

/**
 * Nonce工具类（SDK版本）
 * SDK独立实现，纯Java，不依赖Spring和Redis
 *
 * 注意：此工具类仅用于生成Nonce，不负责验证
 * Nonce的验证由服务端负责（服务端会检查Redis缓存防止重放攻击）
 *
 * @author zhangzhenhui
 */
public class NonceUtil {

    /**
     * 生成唯一Nonce
     * @return Nonce字符串（32位UUID，去除横线）
     */
    public static String generateNonce() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}