package com.org.api_web.config;

import com.org.api_web.interceptor.JwtInterceptor;
import com.org.api_web.interceptor.ApiPermissionInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * WebMvc配置（拦截器注册）
 * @author zhangzhenhui
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private JwtInterceptor jwtInterceptor;

    @Autowired
    private ApiPermissionInterceptor apiPermissionInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                // 拦截管理后台接口和需要Token验证的用户接口
                .addPathPatterns(
                        "/api/admin/**",                          // 管理后台接口
                        "/api/user/generateNewAkSkByToken",        // 通过Token生成新AK/SK（需要Token验证）
                        "/api/user/getAkStatusByToken",            // 通过Token查询AK状态（需要Token验证）
                        "/api/user/myApiPermissions",              // 查询我的接口权限（需要Token验证）
                        "/api/user/availableApis",                 // 查询可用接口列表（需要Token验证）
                        "/api/user/applyApiPermission",            // 申请开通接口权限（需要Token验证）
                        "/api/user/myStatistics",                  // 查询我的调用统计（需要Token验证）
                        "/api/user/call-log/**",                   // 查询我的调用日志（需要Token验证）
                        "/api/auth/userInfo",                     // 获取用户信息（需要Token验证）
                        "/api/auth/changePassword",                // 修改密码（需要Token验证）
                        "/api/sdk/**"                              // SDK下载接口（需要Token验证）
                )
                // 白名单（不需要Token的接口）
                .excludePathPatterns(
                        "/api/auth/register",     // 注册接口（不需要Token）
                        "/api/auth/login",        // 登录接口（不需要Token）
                        "/api/auth/logout",       // 退出登录接口（不需要Token）
                        "/api/user/verifyAkSk",   // AK/SK校验（SDK调用）
                        "/error"                  // 错误页面
                );

        // 注册业务接口权限拦截器（拦截业务接口调用）
        registry.addInterceptor(apiPermissionInterceptor)
                .addPathPatterns("/api/business/**")  // 拦截所有业务接口
                .excludePathPatterns("/error");        // 排除错误页面
    }
}