package com.org.api_admin_service.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.org.api_common.entity.ApiInfo;
import com.org.api_common.dto.ApiInfoDTO;
import com.org.api_common.vo.ApiInfoVO;

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

    // ========== 接口管理CRUD方法 ==========

    /**
     * 添加接口
     */
    ApiInfoVO addApi(ApiInfoDTO apiInfoDTO);

    /**
     * 修改接口
     */
    ApiInfoVO updateApi(Long id, ApiInfoDTO apiInfoDTO);

    /**
     * 删除接口（逻辑删除）
     */
    void deleteApi(Long id);

    /**
     * 分页查询接口列表
     */
    IPage<ApiInfoVO> pageApiList(Integer pageNum, Integer pageSize,
                                 String apiName, String apiPath, Integer status);

    /**
     * 根据ID查询接口详情
     */
    ApiInfoVO getApiById(Long id);

    /**
     * 启用/禁用接口
     */
    ApiInfoVO updateApiStatus(Long id, Integer status);
}