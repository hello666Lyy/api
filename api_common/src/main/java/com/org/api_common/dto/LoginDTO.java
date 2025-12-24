package com.org.api_common.dto;

import lombok.Data;

/**
 * 登录请求参数
 * @author zhangzhenhui
 */
@Data
public class LoginDTO {
    /** 用户名 */
    private String username;
    /** 密码 */
    private String password;
}