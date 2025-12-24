package com.org.api_common.util;

import com.org.api_common.constant.ErrorCodeEnum;
import com.org.api_common.exception.BusinessException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * JWT令牌工具
 * @author zhangzhenhui
 */
public class JwtUtil {

    /** JWT秘钥（建议配置在配置文件） */
    private static final String JWT_SECRET = "api-platform-jwt-secret-zhangzhenhui-2025";
    /** 令牌有效期：2小时（毫秒） */
    private static final long JWT_EXPIRE_MS = 2 * 60 * 60 * 1000L;

    /**
     * 生成JWT令牌
     * @param claims 自定义载荷
     * @return 令牌串
     */
    public static String generateToken(Map<String, Object> claims) {
        try {
            SecretKey secretKey = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
            return Jwts.builder()
                    .setClaims(claims)
                    .setExpiration(new Date(System.currentTimeMillis() + JWT_EXPIRE_MS))
                    .signWith(secretKey, SignatureAlgorithm.HS256)
                    .compact();
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.SERVER_ERROR, "令牌生成失败：" + e.getMessage());
        }
    }

    /**
     * 解析JWT令牌
     * @param token 令牌串
     * @return 载荷信息
     */
    public static Claims parseToken(String token) {
        try {
            SecretKey secretKey = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
            return Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            throw new BusinessException(ErrorCodeEnum.SIGN_ERROR, "令牌已过期");
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.SIGN_ERROR, "令牌解析失败：" + e.getMessage());
        }
    }

    /**
     * 获取令牌中的用户ID
     * @param token 令牌串
     * @return 用户ID
     */
    public static Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("userId", Long.class);
    }
}
