package com.org.api_common.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 可用接口VO（带开通状态）
 */
@Data
public class AvailableApiVO {
    private Long id;
    private String apiName;
    private String apiPath;
    private String method;
    private String apiDesc;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    
    /** 用户是否已开通该接口 */
    private Boolean hasPermission;
    
    /** 权限过期时间（如果已开通） */
    private LocalDateTime expireTime;
}

