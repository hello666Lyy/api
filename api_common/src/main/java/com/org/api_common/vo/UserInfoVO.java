package com.org.api_common.vo;

import lombok.Data;

/**
 * 用户信息返回VO（隐藏SK等敏感字段）
 * api_common层：公共返回对象，供所有模块复用
 */
@Data
public class UserInfoVO {
    /** 用户ID */
    private Long id;
    /** 访问密钥AK（仅展示，不返回SK） */
    private String accessKey;
    /** 用户名 */
    private String username;
    /** 用户状态（1启用，0禁用） */
    private Integer status;
}