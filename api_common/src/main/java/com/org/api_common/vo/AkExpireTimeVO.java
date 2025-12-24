package com.org.api_common.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * AK过期时间管理返回VO
 */
@Data
public class AkExpireTimeVO {
    /** 目标AK */
    private String targetAk;
    /** 过期时间（null=永不过期） */
    private LocalDateTime expireTime;
    /** 是否已过期 */
    private Boolean isExpired;
    /** 过期状态描述（如："未过期（剩余15天）"、"已过期"、"永不过期"） */
    private String expireDesc;
}
