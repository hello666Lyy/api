package com.org.api_web.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.org.api_common.result.Result;
import com.org.api_common.vo.ApiPermissionVO;
import com.org.api_admin_service.service.ApiPermissionService;
import com.org.api_web.interceptor.JwtInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 接口权限控制器（用户端）
 */
@RestController
@RequestMapping("/api/user")
public class UserApiPermissionController {

    @Autowired
    private ApiPermissionService apiPermissionService;

    /**
     * 查询当前用户的接口权限（通过Token）
     */
    @GetMapping("/myApiPermissions")
    public Result<IPage<ApiPermissionVO>> getMyApiPermissions(
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize
    ) {
        // 1. 从Token中获取当前用户ID
        Long userId = JwtInterceptor.getCurrentUserId();
        if (userId == null) {
            throw new com.org.api_common.exception.BusinessException(
                    com.org.api_common.constant.ErrorCodeEnum.TOKEN_MISSING, "Token中未找到用户ID");
        }

        // 2. 查询当前用户的接口权限
        IPage<ApiPermissionVO> page = apiPermissionService.getUserApiPermissions(
                userId, pageNum, pageSize);

        return Result.success(page, "查询成功");
    }
}

