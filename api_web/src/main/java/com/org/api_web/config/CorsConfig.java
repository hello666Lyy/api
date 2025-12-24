package com.org.api_web.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * CORS跨域配置（前后端分离必需）
 * @author zhangzhenhui
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        // 允许的前端域名（开发环境 Vite 默认端口）
        config.addAllowedOrigin("http://localhost:5173");   // 管理后台
        config.addAllowedOrigin("http://127.0.0.1:5173");
        config.addAllowedOrigin("http://localhost:5180");   // 客户端前端
        config.addAllowedOrigin("http://127.0.0.1:5180");
        config.addAllowedOrigin("http://localhost:3000");
        // 内网穿透域名（公网访问时需要）
        config.addAllowedOrigin("https://6b2ea6e4.r17.cpolar.top");
        config.addAllowedOrigin("http://6b2ea6e4.r17.cpolar.top");
        // 生产环境添加你的域名
        // config.addAllowedOrigin("https://your-domain.com");

        // 允许携带Cookie
        config.setAllowCredentials(true);
        // 允许所有请求头
        config.addAllowedHeader("*");
        // 允许所有请求方法
        config.addAllowedMethod("*");
        // 暴露响应头（前端可读取）
        config.addExposedHeader("Authorization");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}