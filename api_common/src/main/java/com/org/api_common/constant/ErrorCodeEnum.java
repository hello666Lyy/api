package com.org.api_common.constant;

import lombok.Getter;

/**
 * 全局错误码枚举
 * @author zhangzhenhui
 */
@Getter
public enum ErrorCodeEnum {

    TOKEN_MISSING(401, "请先登录"),           // 新增
    TOKEN_INVALID(401, "登录已失效，请重新登录"), // 新增
    LOGIN_FAILED(401, "用户名或密码错误"),      // 新增

    RATE_LIMIT_EXCEEDED(429, "请求过于频繁，请稍后再试"),
    IP_RATE_LIMIT_EXCEEDED(429, "IP请求过于频繁，请稍后再试"),
    USER_RATE_LIMIT_EXCEEDED(429, "用户请求过于频繁，请稍后再试"),

    FORBIDDEN(403, "禁止访问"),
    SYSTEM_ERROR(501, "系统错误"),
    SUCCESS(200, "操作成功"),
    PARAM_ERROR(400, "参数错误"),
    SIGN_ERROR(401, "签名验证失败"),
    NONCE_REPEAT(401, "请求重复（nonce已使用）"),
    TIMESTAMP_TIMEOUT(401, "时间戳过期"),
    PERMISSION_DENY(403, "接口权限不足"),
    NOT_FOUND(404, "资源不存在"),
    SERVER_ERROR(500, "服务器内部错误");


    private final int code;
    private final String msg;

    ErrorCodeEnum(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public String getMsg() {
        return msg;
    }

    public int getCode() {
        return code;
    }
}
