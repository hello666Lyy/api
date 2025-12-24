package com.org.api_common.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ApiInfoVO {
    private Long id;
    private String apiName;
    private String apiPath;
    private String method;
    private String apiDesc;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}