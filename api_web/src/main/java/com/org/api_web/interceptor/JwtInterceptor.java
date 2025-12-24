package com.org.api_web.interceptor;

import com.org.api_common.constant.ErrorCodeEnum;
import com.org.api_common.exception.BusinessException;
import com.org.api_common.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT Token 拦截器（管理后台接口鉴权）
 * @author zhangzhenhui
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {

    /** ThreadLocal 存储当前用户信息 */
    private static final ThreadLocal<Claims> CURRENT_USER = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // OPTIONS 预检请求直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 从 Header 获取 Token
        String token = request.getHeader("Authorization");
        if (!StringUtils.hasText(token)) {
            throw new BusinessException(ErrorCodeEnum.TOKEN_MISSING);
        }

        // 去除 Bearer 前缀
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        // 解析并校验 Token（失败会抛异常）
        Claims claims = JwtUtil.parseToken(token);

        // 存储用户信息到 ThreadLocal（Controller 可通过静态方法获取）
        CURRENT_USER.set(claims);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 请求结束清理 ThreadLocal，避免内存泄漏
        CURRENT_USER.remove();
    }

    /** 获取当前登录用户ID */
    public static Long getCurrentUserId() {
        Claims claims = CURRENT_USER.get();
        return claims != null ? claims.get("userId", Long.class) : null;
    }

    /** 获取当前登录用户名 */
    public static String getCurrentUsername() {
        Claims claims = CURRENT_USER.get();
        return claims != null ? claims.get("username", String.class) : null;
    }

    /** 获取当前用户 AccessKey */
    public static String getCurrentAccessKey() {
        Claims claims = CURRENT_USER.get();
        return claims != null ? claims.get("accessKey", String.class) : null;
    }
}