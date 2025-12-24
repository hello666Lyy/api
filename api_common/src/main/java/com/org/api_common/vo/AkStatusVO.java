package com.org.api_common.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * AK状态返回VO
 */
@Data
public class AkStatusVO {
    /** 用户/AK关联ID */
    private Long id;
    /** 访问密钥AK */
    private String accessKey;
    /** AK状态（1=启用，0=禁用） */
    private Integer status;
    /** AK最后更新时间（生成/禁用/启用操作都会更新） */
    private LocalDateTime updateTime;
}