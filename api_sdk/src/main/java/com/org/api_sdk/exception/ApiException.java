package com.org.api_sdk.exception;

import lombok.Getter;

/**
 * SDK异常类
 * @author zhangzhenhui
 */
@Getter
public class ApiException extends RuntimeException {
    private final int code;
    private final String msg;

    public ApiException(int code, String msg) {
        super(msg);
        this.code = code;
        this.msg = msg;
    }

    public ApiException(String msg) {
        super(msg);
        this.code = 500;
        this.msg = msg;
    }

    public ApiException(String msg, Throwable cause) {
        super(msg, cause);
        this.code = 500;
        this.msg = msg;
    }
}