package com.org.api_common.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 接口权限VO
 */
@Data
public class ApiPermissionVO {
    private Long id;
    private Long userId;
    private Long apiId;
    private String apiName;
    private String apiPath;
    private String method;
    private Integer status; // 0=未授权 1=已授权
    private LocalDateTime expireTime;
    private LocalDateTime createTime;
}

