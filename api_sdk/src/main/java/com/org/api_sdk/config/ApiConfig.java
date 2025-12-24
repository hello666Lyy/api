package com.org.api_sdk.config;

import lombok.Builder;
import lombok.Data;

/**
 * SDK配置类
 * @author zhangzhenhui
 */
@Data
@Builder
public class ApiConfig {
    /** API服务基础URL（如：http://localhost:8080） */
    private String baseUrl;

    /** 访问密钥（AccessKey） */
    private String accessKey;

    /** 密钥（SecretKey） */
    private String secretKey;

    /** 连接超时时间（毫秒），默认5秒 */
    @Builder.Default
    private int connectTimeout = 5000;

    /** 读取超时时间（毫秒），默认10秒 */
    @Builder.Default
    private int readTimeout = 10000;

    /** 是否打印请求日志，默认false */
    @Builder.Default
    private boolean enableLog = false;
}