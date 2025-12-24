package com.org.api_common.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 单个AK状态返回VO
 */
@Data
public class SingleAkStatusVO {
    /** 待查询的AK */
    private String accessKey;
    /** 是否存在（true=存在，false=不存在） */
    private Boolean exist;
    /** AK状态（1=启用，0=禁用；不存在则为null） */
    private Integer status;
    /** 最后更新时间（不存在则为null，统一用LocalDateTime） */
    private LocalDateTime updateTime;
}