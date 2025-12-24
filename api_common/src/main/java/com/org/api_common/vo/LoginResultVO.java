package com.org.api_common.vo;

import lombok.Data;

/**
 * 登录响应结果
 * @author zhangzhenhui
 */
@Data
public class LoginResultVO {
    /** JWT Token */
    private String token;
    /** 用户ID */
    private Long userId;
    /** 用户名 */
    private String username;
    /** AccessKey */
    private String accessKey;
    /** 权限类型：1=只读，2=读写，3=管理员 */
    private Integer permissionType;
    /** Token 过期时间（毫秒时间戳） */
    private Long expireTime;
    /** SecretKey（注册时返回，仅此一次） */
    private String secretKey;
}