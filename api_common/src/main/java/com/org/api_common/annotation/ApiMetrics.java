package com.org.api_common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * API指标监控注解
 * 标记在Controller方法上，用于统计接口调用数据
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiMetrics {
    /**
     * 是否记录请求参数（默认true）
     */
    boolean recordParams() default true;

    /**
     * 是否记录响应结果（默认false，避免数据过大）
     */
    boolean recordResponse() default false;

    /**
     * 接口名称（可选，默认使用路径）
     */
    String value() default "";
}