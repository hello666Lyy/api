package com.org.api_common.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class ApiInfoDTO {
    @NotBlank(message = "接口名称不能为空")
    private String apiName;

    @NotBlank(message = "接口路径不能为空")
    private String apiPath;

    @NotBlank(message = "请求方式不能为空")
    private String method;

    private String apiDesc;

    @NotNull(message = "状态不能为空")
    private Integer status;
}