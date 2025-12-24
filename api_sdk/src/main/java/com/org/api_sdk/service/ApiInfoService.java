package com.org.api_sdk.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.org.api_common.vo.ApiInfoVO;
import com.org.api_sdk.ApiClient;

import java.util.Map;

/**
 * 接口管理API服务
 * @author zhangzhenhui
 */
public class ApiInfoService {
    private final ApiClient client;

    public ApiInfoService(ApiClient client) {
        this.client = client;
    }

    /**
     * 分页查询接口列表
     */
    public IPage<ApiInfoVO> listApis(Integer pageNum, Integer pageSize,
                                     String apiName, String apiPath, Integer status) {
        Map<String, Object> params = new java.util.HashMap<>();
        if (pageNum != null) {
            params.put("pageNum", pageNum);
        }
        if (pageSize != null) {
            params.put("pageSize", pageSize);
        }
        if (apiName != null) {
            params.put("apiName", apiName);
        }
        if (apiPath != null) {
            params.put("apiPath", apiPath);
        }
        if (status != null) {
            params.put("status", status);
        }
        return client.executeRequest("GET", "/api/admin/api-info/list", params, IPage.class);
    }

    /**
     * 添加接口
     */
    public ApiInfoVO addApi(String apiName, String apiPath, String method,
                            String apiDesc, Integer status) {
        Map<String, Object> params = new java.util.HashMap<>();
        params.put("apiName", apiName);
        params.put("apiPath", apiPath);
        params.put("method", method);
        if (apiDesc != null) {
            params.put("apiDesc", apiDesc);
        }
        params.put("status", status);
        return client.executeRequest("POST", "/api/admin/api-info/add", params, ApiInfoVO.class);
    }

    /**
     * 修改接口
     */
    public ApiInfoVO updateApi(Long id, String apiName, String apiPath, String method,
                               String apiDesc, Integer status) {
        Map<String, Object> params = new java.util.HashMap<>();
        params.put("apiName", apiName);
        params.put("apiPath", apiPath);
        params.put("method", method);
        if (apiDesc != null) {
            params.put("apiDesc", apiDesc);
        }
        params.put("status", status);
        return client.executeRequest("PUT", "/api/admin/api-info/update/" + id, params, ApiInfoVO.class);
    }

    /**
     * 删除接口
     */
    public Void deleteApi(Long id) {
        return client.executeRequest("DELETE", "/api/admin/api-info/delete/" + id, null, Void.class);
    }

    /**
     * 获取接口详情
     */
    public ApiInfoVO getApiDetail(Long id) {
        return client.executeRequest("GET", "/api/admin/api-info/detail/" + id, null, ApiInfoVO.class);
    }

    /**
     * 启用/禁用接口
     * @param id 接口ID
     * @param status 状态：1=启用，0=禁用
     */
    public ApiInfoVO updateApiStatus(Long id, Integer status) {
        Map<String, Object> params = new java.util.HashMap<>();
        params.put("status", status);
        return client.executeRequest("PUT", "/api/admin/api-info/status/" + id, params, ApiInfoVO.class);
    }
}