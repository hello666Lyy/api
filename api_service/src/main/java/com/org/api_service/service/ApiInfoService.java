package com.org.api_service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.org.api_common.entity.ApiInfo;

/**
 * 接口信息业务接口
 */
public interface ApiInfoService extends IService<ApiInfo> {
    /**
     * 根据接口路径+请求方式查询有效接口
     */
    ApiInfo getValidApiByPathAndMethod(String apiPath, String method);

    /**
     * 校验接口是否存在且可用
     */
    void checkApiAvailable(String apiPath, String method);
}