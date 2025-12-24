package com.org.api_web.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.org.api_common.entity.ApiInfo;
import com.org.api_common.entity.SysUser;
import com.org.api_common.result.Result;
import com.org.api_common.vo.ApiPermissionVO;
import com.org.api_common.vo.AvailableApiVO;
import com.org.api_admin_service.service.ApiInfoService;
import com.org.api_admin_service.service.ApiPermissionService;
import com.org.api_service.service.SysUserService;
import com.org.api_web.interceptor.JwtInterceptor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 接口市场控制器（用户端）
 */
@RestController
@RequestMapping("/api/user")
public class ApiMarketController {

    @Autowired
    private ApiInfoService apiInfoService;

    @Autowired
    private ApiPermissionService apiPermissionService;

    @Autowired
    private SysUserService sysUserService;

    /**
     * 查询所有可用接口（带开通状态）
     * 需要Token认证
     */
    @GetMapping("/availableApis")
    public Result<IPage<AvailableApiVO>> getAvailableApis(
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String apiName
    ) {
        // 1. 从Token中获取当前用户ID
        Long userId = JwtInterceptor.getCurrentUserId();
        if (userId == null) {
            throw new com.org.api_common.exception.BusinessException(
                    com.org.api_common.constant.ErrorCodeEnum.TOKEN_MISSING, "Token中未找到用户ID");
        }

        // 2. 设置默认值
        if (pageNum == null || pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }

        // 3. 查询所有启用的业务接口（路径以 /api/business 开头）
        Page<ApiInfo> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ApiInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiInfo::getStatus, 1) // 只查询启用的接口
                .like(ApiInfo::getApiPath, "/api/business") // 只查询业务接口
                .orderByDesc(ApiInfo::getCreateTime);

        if (apiName != null && !apiName.isEmpty()) {
            wrapper.like(ApiInfo::getApiName, apiName);
        }

        IPage<ApiInfo> apiInfoPage = apiInfoService.page(page, wrapper);

        // 4. 查询用户已开通的接口权限
        List<ApiPermissionVO> userPermissions = apiPermissionService.getUserApiPermissions(userId);
        Map<Long, ApiPermissionVO> permissionMap = userPermissions.stream()
                .collect(Collectors.toMap(ApiPermissionVO::getApiId, p -> p));

        // 5. 转换为VO，并标记是否已开通
        List<AvailableApiVO> voList = apiInfoPage.getRecords().stream().map(apiInfo -> {
            AvailableApiVO vo = new AvailableApiVO();
            BeanUtils.copyProperties(apiInfo, vo);
            
            ApiPermissionVO permission = permissionMap.get(apiInfo.getId());
            if (permission != null && permission.getStatus() == 1) {
                // 检查是否过期
                if (permission.getExpireTime() == null || permission.getExpireTime().isAfter(LocalDateTime.now())) {
                    vo.setHasPermission(true);
                    vo.setExpireTime(permission.getExpireTime());
                } else {
                    vo.setHasPermission(false);
                }
            } else {
                vo.setHasPermission(false);
            }
            
            return vo;
        }).collect(Collectors.toList());

        // 6. 构建分页结果
        Page<AvailableApiVO> voPage = new Page<>(pageNum, pageSize);
        voPage.setRecords(voList);
        voPage.setTotal(apiInfoPage.getTotal());
        voPage.setPages(apiInfoPage.getPages());

        return Result.success(voPage, "查询成功");
    }

    /**
     * 用户自主申请开通接口权限（自动开通）
     * 需要Token认证
     */
    @PostMapping("/applyApiPermission")
    public Result<Map<String, Object>> applyApiPermission(
            @RequestParam Long apiId,
            @RequestParam(required = false) String expireTime
    ) {
        // 1. 从Token中获取当前用户ID
        Long userId = JwtInterceptor.getCurrentUserId();
        if (userId == null) {
            throw new com.org.api_common.exception.BusinessException(
                    com.org.api_common.constant.ErrorCodeEnum.TOKEN_MISSING, "Token中未找到用户ID");
        }

        // 2. 校验接口是否存在且启用
        ApiInfo apiInfo = apiInfoService.getById(apiId);
        if (apiInfo == null) {
            throw new com.org.api_common.exception.BusinessException(
                    com.org.api_common.constant.ErrorCodeEnum.NOT_FOUND, "接口不存在");
        }
        if (apiInfo.getStatus() != 1) {
            throw new com.org.api_common.exception.BusinessException(
                    com.org.api_common.constant.ErrorCodeEnum.PARAM_ERROR, "接口已禁用");
        }

        // 3. 检查是否已开通
        List<ApiPermissionVO> userPermissions = apiPermissionService.getUserApiPermissions(userId);
        boolean alreadyHasPermission = userPermissions.stream()
                .anyMatch(p -> p.getApiId().equals(apiId)
                        && p.getStatus() == 1
                        && (p.getExpireTime() == null || p.getExpireTime().isAfter(LocalDateTime.now())));

        if (alreadyHasPermission) {
            return Result.success(null, "您已开通该接口权限");
        }

        // 4. 解析过期时间
        LocalDateTime expireTimeObj = null;
        if (expireTime != null && !expireTime.isEmpty()) {
            try {
                expireTimeObj = LocalDateTime.parse(expireTime);
            } catch (Exception e) {
                throw new com.org.api_common.exception.BusinessException(
                        com.org.api_common.constant.ErrorCodeEnum.PARAM_ERROR, "过期时间格式错误");
            }
        }

        // 5. 自动开通权限
        int successCount = apiPermissionService.grantApiPermission(userId, new Long[]{apiId}, expireTimeObj);

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("success", successCount > 0);
        result.put("apiId", apiId);
        result.put("apiName", apiInfo.getApiName());

        return Result.success(result, "接口权限开通成功");
    }
}

