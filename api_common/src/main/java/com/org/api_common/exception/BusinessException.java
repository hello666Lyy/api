package com.org.api_common.exception;

import com.org.api_common.constant.ErrorCodeEnum;
import lombok.Data;
import lombok.Getter;

/**
 * 全局业务异常
 * @author zhangzhenhui
 */
//@Data
@Getter
public class BusinessException extends RuntimeException {
    private final int code;
    private final String msg;

    public BusinessException(ErrorCodeEnum errorCode) {
        super(errorCode.getMsg());
        this.code = errorCode.getCode();
        this.msg = errorCode.getMsg();
    }

    public BusinessException(int code, String msg) {
        super(msg);
        this.code = code;
        this.msg = msg;
    }

//    // 新增构造函数1：直接接收ErrorCodeEnum
//    public BusinessException(ErrorCodeEnum errorCode) {
//        this(errorCode.getCode(), errorCode.getMsg()); // 调用原构造函数
//    }

    // 新增构造函数2：接收ErrorCodeEnum + 自定义补充信息（你现在需要的场景）
    public BusinessException(ErrorCodeEnum errorCode, String extraMsg) {
        // 把枚举的msg和自定义msg拼接，code用枚举的code
        this(errorCode.getCode(), errorCode.getMsg() + "：" + extraMsg);
    }
}
