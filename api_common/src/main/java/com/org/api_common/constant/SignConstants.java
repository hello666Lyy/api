package com.org.api_common.constant;

/**
 * 签名算法常量
 * @author zhangzhenhui
 */
public class SignConstants {
    /** AK启用状态 */
    public static final int AK_STATUS_ENABLE = 1;
    /** AK禁用状态 */
    public static final int AK_STATUS_DISABLE = 0;
    /** AK长度（16位） */
    public static final int AK_LENGTH = 16;
    /** SK长度（32位） */
    public static final int SK_LENGTH = 32;
    /** 签名算法：HMAC-SHA256 */
    public static final String SIGN_ALGORITHM = "HmacSHA256";
    /** 时间戳有效期：5分钟（秒） */
    public static final long TIMESTAMP_EXPIRE_SECOND = 300L;
    /** Nonce有效期：5分钟（秒） */
    public static final long NONCE_EXPIRE_SECOND = 300L;
    /** 签名参数排序规则：ASCII升序 */
    public static final String SIGN_SORT_RULE = "ASCII";
}