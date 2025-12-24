package com.org.api_admin_service.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.org.api_common.entity.ApiPermission;
import com.org.api_common.vo.ApiPermissionVO;

/**
 * 接口权限业务接口
 */
public interface ApiPermissionService extends IService<ApiPermission> {

    /**
     * 为用户开通接口权限
     * @param userId 用户ID
     * @param apiIds 接口ID列表
     * @param expireTime 过期时间（可选）
     * @return 成功开通的数量
     */
    int grantApiPermission(Long userId, Long[] apiIds, java.time.LocalDateTime expireTime);

    /**
     * 撤销用户接口权限
     * @param userId 用户ID
     * @param apiIds 接口ID列表
     * @return 成功撤销的数量
     */
    int revokeApiPermission(Long userId, Long[] apiIds);

    /**
     * 查询用户已开通的接口权限列表（分页）
     * @param userId 用户ID
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 分页结果
     */
    IPage<ApiPermissionVO> getUserApiPermissions(Long userId, Integer pageNum, Integer pageSize);

    /**
     * 查询用户已开通的接口权限列表（不分页，用于校验）
     * @param userId 用户ID
     * @return 权限列表
     */
    java.util.List<ApiPermissionVO> getUserApiPermissions(Long userId);
}

