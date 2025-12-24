package com.org.api_common.constant;

/**
 * 缓存Key常量
 * @author zhangzhenhui
 */
public class CacheKeyConstants {

    /** API调用日志队列 */
    public static final String API_METRICS_QUEUE = "api:metrics:queue";

    /** API调用总次数统计 */
    public static final String API_METRICS_TOTAL = "api:metrics:total";

    /** API调用成功次数统计 */
    public static final String API_METRICS_SUCCESS = "api:metrics:success";

    /** API调用失败次数统计 */
    public static final String API_METRICS_FAIL = "api:metrics:fail";

    /** 用户调用次数统计 */
    public static final String API_METRICS_USER = "api:metrics:user";

    /** API调用累计响应时间统计 */
    public static final String API_METRICS_TIME = "api:metrics:time";


    /** 用户SK缓存前缀：api:sk:{ak} */
    public static final String USER_SK_PREFIX = "api:sk:";
    /** Nonce防重放缓存前缀：api:nonce:{ak}:{nonce} */
    public static final String NONCE_PREFIX = "api:nonce:";
    /** 接口权限缓存前缀：api:permission:{userId}:{apiId} */
    public static final String API_PERMISSION_PREFIX = "api:permission:";
    /** IP限流缓存前缀：api:limit:ip:{ip} */
    public static final String IP_LIMIT_PREFIX = "api:limit:ip:";
}
