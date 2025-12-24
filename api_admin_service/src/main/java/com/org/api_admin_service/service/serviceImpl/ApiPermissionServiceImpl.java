package com.org.api_admin_service.service.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.org.api_admin_service.mapper.ApiPermissionMapper;
import com.org.api_admin_service.service.ApiPermissionService;
import com.org.api_admin_service.service.ApiInfoService;
import com.org.api_common.constant.ErrorCodeEnum;
import com.org.api_common.entity.ApiPermission;
import com.org.api_common.entity.ApiInfo;
import com.org.api_common.exception.BusinessException;
import com.org.api_common.vo.ApiPermissionVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 接口权限业务实现
 */
@Service
public class ApiPermissionServiceImpl extends ServiceImpl<ApiPermissionMapper, ApiPermission>
        implements ApiPermissionService {

    @Autowired
    private ApiInfoService apiInfoService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int grantApiPermission(Long userId, Long[] apiIds, LocalDateTime expireTime) {
        if (userId == null || apiIds == null || apiIds.length == 0) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR, "参数错误");
        }

        // 1. 校验接口是否存在且启用
        for (Long apiId : apiIds) {
            ApiInfo apiInfo = apiInfoService.getById(apiId);
            if (apiInfo == null) {
                throw new BusinessException(ErrorCodeEnum.NOT_FOUND, "接口不存在，ID: " + apiId);
            }
            if (apiInfo.getStatus() != 1) {
                throw new BusinessException(ErrorCodeEnum.PARAM_ERROR, "接口已禁用，ID: " + apiId);
            }
        }

        // 3. 批量开通权限
        int successCount = 0;
        for (Long apiId : apiIds) {
            // 查询是否已存在权限记录
            LambdaQueryWrapper<ApiPermission> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ApiPermission::getUserId, userId)
                    .eq(ApiPermission::getApiId, apiId);
            ApiPermission existing = this.getOne(wrapper);

            if (existing != null) {
                // 如果已存在，更新为已授权状态
                existing.setStatus(1);
                existing.setExpireTime(expireTime);
                existing.setCreateTime(LocalDateTime.now());
                this.updateById(existing);
            } else {
                // 如果不存在，创建新记录
                ApiPermission permission = new ApiPermission();
                permission.setUserId(userId);
                permission.setApiId(apiId);
                permission.setStatus(1);
                permission.setExpireTime(expireTime);
                permission.setCreateTime(LocalDateTime.now());
                this.save(permission);
            }
            successCount++;
        }

        return successCount;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int revokeApiPermission(Long userId, Long[] apiIds) {
        if (userId == null || apiIds == null || apiIds.length == 0) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR, "参数错误");
        }

        // 批量撤销权限（逻辑删除：将status设置为0）
        int successCount = 0;
        for (Long apiId : apiIds) {
            LambdaQueryWrapper<ApiPermission> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ApiPermission::getUserId, userId)
                    .eq(ApiPermission::getApiId, apiId)
                    .eq(ApiPermission::getStatus, 1); // 只撤销已授权的权限

            ApiPermission permission = this.getOne(wrapper);
            if (permission != null) {
                permission.setStatus(0);
                this.updateById(permission);
                successCount++;
            }
        }

        return successCount;
    }

    @Override
    public IPage<ApiPermissionVO> getUserApiPermissions(Long userId, Integer pageNum, Integer pageSize) {
        // 设置默认值
        if (pageNum == null || pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }

        // 1. 分页查询权限记录（只查询已授权的）
        Page<ApiPermission> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ApiPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiPermission::getUserId, userId)
                .eq(ApiPermission::getStatus, 1) // 只查询已授权的
                .orderByDesc(ApiPermission::getCreateTime);

        // 过滤已过期的权限
        wrapper.and(w -> w.isNull(ApiPermission::getExpireTime)
                .or()
                .gt(ApiPermission::getExpireTime, LocalDateTime.now()));

        IPage<ApiPermission> permissionPage = this.page(page, wrapper);

        // 2. 获取所有接口ID
        List<Long> apiIds = permissionPage.getRecords().stream()
                .map(ApiPermission::getApiId)
                .collect(Collectors.toList());

        // 3. 批量查询接口信息
        Map<Long, ApiInfo> apiInfoMap = new java.util.HashMap<>();
        if (!apiIds.isEmpty()) {
            apiInfoMap = apiInfoService.listByIds(apiIds).stream()
                    .collect(Collectors.toMap(ApiInfo::getId, api -> api));
        }

        // 4. 转换为VO
        List<ApiPermissionVO> voList = new ArrayList<>();
        for (ApiPermission permission : permissionPage.getRecords()) {
            ApiInfo apiInfo = apiInfoMap.get(permission.getApiId());
            if (apiInfo != null) {
                ApiPermissionVO vo = new ApiPermissionVO();
                vo.setId(permission.getId());
                vo.setUserId(permission.getUserId());
                vo.setApiId(permission.getApiId());
                vo.setApiName(apiInfo.getApiName());
                vo.setApiPath(apiInfo.getApiPath());
                vo.setMethod(apiInfo.getMethod());
                vo.setStatus(permission.getStatus());
                vo.setExpireTime(permission.getExpireTime());
                vo.setCreateTime(permission.getCreateTime());
                voList.add(vo);
            }
        }

        // 5. 构建分页结果
        Page<ApiPermissionVO> voPage = new Page<>(pageNum, pageSize);
        voPage.setRecords(voList);
        voPage.setTotal(permissionPage.getTotal());
        voPage.setPages(permissionPage.getPages());

        return voPage;
    }

    @Override
    public List<ApiPermissionVO> getUserApiPermissions(Long userId) {
        // 查询所有已授权且未过期的权限
        LambdaQueryWrapper<ApiPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiPermission::getUserId, userId)
                .eq(ApiPermission::getStatus, 1)
                .and(w -> w.isNull(ApiPermission::getExpireTime)
                        .or()
                        .gt(ApiPermission::getExpireTime, LocalDateTime.now()))
                .orderByDesc(ApiPermission::getCreateTime);

        List<ApiPermission> permissions = this.list(wrapper);

        // 获取接口信息
        List<Long> apiIds = permissions.stream()
                .map(ApiPermission::getApiId)
                .collect(Collectors.toList());

        if (apiIds.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Long, ApiInfo> apiInfoMap = apiInfoService.listByIds(apiIds).stream()
                .collect(Collectors.toMap(ApiInfo::getId, api -> api));

        // 转换为VO
        List<ApiPermissionVO> voList = new ArrayList<>();
        for (ApiPermission permission : permissions) {
            ApiInfo apiInfo = apiInfoMap.get(permission.getApiId());
            if (apiInfo != null) {
                ApiPermissionVO vo = new ApiPermissionVO();
                vo.setId(permission.getId());
                vo.setUserId(permission.getUserId());
                vo.setApiId(permission.getApiId());
                vo.setApiName(apiInfo.getApiName());
                vo.setApiPath(apiInfo.getApiPath());
                vo.setMethod(apiInfo.getMethod());
                vo.setStatus(permission.getStatus());
                vo.setExpireTime(permission.getExpireTime());
                vo.setCreateTime(permission.getCreateTime());
                voList.add(vo);
            }
        }

        return voList;
    }
}

