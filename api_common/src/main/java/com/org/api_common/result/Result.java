package com.org.api_common.result;

import com.org.api_common.constant.ErrorCodeEnum;
import lombok.Data;

/**
 * 全局统一返回结果
 * @author zhangzhenhui
 * @param <T> 数据类型
 */
@Data
public class Result<T> {
    /** 响应码 */
    private int code;
    /** 响应信息 */
    private String msg;
    /** 响应数据 */
    private T data;

    // 新增：支持“带数据+自定义响应消息”的成功响应（匹配你当前的调用方式）
    public static <T> Result<T> success(T data, String msg) {
        // 用SUCCESS的响应码，自定义的msg，传入的data
        return new Result<>(ErrorCodeEnum.SUCCESS.getCode(), msg, data);
    }

    // 成功响应（无数据）
    public static <T> Result<T> success() {
        return new Result<>(ErrorCodeEnum.SUCCESS.getCode(), ErrorCodeEnum.SUCCESS.getMsg(), null);
    }

    // 成功响应（有数据）
    public static <T> Result<T> success(T data) {
        return new Result<>(ErrorCodeEnum.SUCCESS.getCode(), ErrorCodeEnum.SUCCESS.getMsg(), data);
    }

    // 失败响应（枚举）
    public static <T> Result<T> fail(ErrorCodeEnum errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMsg(), null);
    }

    // 失败响应（自定义）
    public static <T> Result<T> fail(int code, String msg) {
        return new Result<>(code, msg, null);
    }

    private Result(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }
}
