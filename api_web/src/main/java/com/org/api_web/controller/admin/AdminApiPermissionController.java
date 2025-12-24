package com.org.api_web.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.org.api_common.result.Result;
import com.org.api_common.vo.ApiPermissionVO;
import com.org.api_admin_service.service.ApiPermissionService;
import com.org.api_service.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 接口权限管理控制器（管理员专用）
 */
@RestController
@RequestMapping("/api/admin/api-permission")
public class AdminApiPermissionController {

    @Autowired
    private ApiPermissionService apiPermissionService;

    @Autowired
    private SysUserService sysUserService;

    /**
     * 为用户开通接口权限
     */
    @PostMapping("/grant")
    public Result<Map<String, Integer>> grantApiPermission(
            @RequestParam String accessKey,
            @RequestParam String sign,
            @RequestParam Long timestamp,
            @RequestParam String nonce,
            @RequestParam String targetAk,
            @RequestParam String apiIds, // 逗号分隔的接口ID列表，如 "1,2,3"
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime expireTime
    ) {
        // 1. 管理员权限校验
        sysUserService.verifyAdminPermission(accessKey, sign, timestamp, nonce);

        // 2. 根据targetAk获取用户ID
        com.org.api_common.entity.SysUser targetUser = sysUserService.getByAccessKey(targetAk);
        if (targetUser == null) {
            throw new com.org.api_common.exception.BusinessException(
                    com.org.api_common.constant.ErrorCodeEnum.NOT_FOUND, "目标用户不存在");
        }

        // 3. 解析接口ID列表
        String[] apiIdStrs = apiIds.split(",");
        Long[] apiIdArray = new Long[apiIdStrs.length];
        for (int i = 0; i < apiIdStrs.length; i++) {
            try {
                apiIdArray[i] = Long.parseLong(apiIdStrs[i].trim());
            } catch (NumberFormatException e) {
                throw new com.org.api_common.exception.BusinessException(
                        com.org.api_common.constant.ErrorCodeEnum.PARAM_ERROR, "接口ID格式错误: " + apiIdStrs[i]);
            }
        }

        // 4. 开通权限
        int successCount = apiPermissionService.grantApiPermission(
                targetUser.getId(), apiIdArray, expireTime);

        Map<String, Integer> result = new HashMap<>();
        result.put("successCount", successCount);
        result.put("failedCount", apiIdArray.length - successCount);

        return Result.success(result, "权限开通成功");
    }

    /**
     * 撤销用户接口权限
     */
    @PostMapping("/revoke")
    public Result<Map<String, Integer>> revokeApiPermission(
            @RequestParam String accessKey,
            @RequestParam String sign,
            @RequestParam Long timestamp,
            @RequestParam String nonce,
            @RequestParam String targetAk,
            @RequestParam String apiIds // 逗号分隔的接口ID列表
    ) {
        // 1. 管理员权限校验
        sysUserService.verifyAdminPermission(accessKey, sign, timestamp, nonce);

        // 2. 根据targetAk获取用户ID
        com.org.api_common.entity.SysUser targetUser = sysUserService.getByAccessKey(targetAk);
        if (targetUser == null) {
            throw new com.org.api_common.exception.BusinessException(
                    com.org.api_common.constant.ErrorCodeEnum.NOT_FOUND, "目标用户不存在");
        }

        // 3. 解析接口ID列表
        String[] apiIdStrs = apiIds.split(",");
        Long[] apiIdArray = new Long[apiIdStrs.length];
        for (int i = 0; i < apiIdStrs.length; i++) {
            try {
                apiIdArray[i] = Long.parseLong(apiIdStrs[i].trim());
            } catch (NumberFormatException e) {
                throw new com.org.api_common.exception.BusinessException(
                        com.org.api_common.constant.ErrorCodeEnum.PARAM_ERROR, "接口ID格式错误: " + apiIdStrs[i]);
            }
        }

        // 4. 撤销权限
        int successCount = apiPermissionService.revokeApiPermission(
                targetUser.getId(), apiIdArray);

        Map<String, Integer> result = new HashMap<>();
        result.put("successCount", successCount);
        result.put("failedCount", apiIdArray.length - successCount);

        return Result.success(result, "权限撤销成功");
    }

    /**
     * 查询用户已开通的接口权限列表（管理员）
     */
    @GetMapping("/userApis")
    public Result<IPage<ApiPermissionVO>> getUserApiPermissions(
            @RequestParam String accessKey,
            @RequestParam String sign,
            @RequestParam Long timestamp,
            @RequestParam String nonce,
            @RequestParam String targetAk,
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize
    ) {
        // 1. 管理员权限校验
        sysUserService.verifyAdminPermission(accessKey, sign, timestamp, nonce);

        // 2. 根据targetAk获取用户ID
        com.org.api_common.entity.SysUser targetUser = sysUserService.getByAccessKey(targetAk);
        if (targetUser == null) {
            throw new com.org.api_common.exception.BusinessException(
                    com.org.api_common.constant.ErrorCodeEnum.NOT_FOUND, "目标用户不存在");
        }

        // 3. 查询权限列表
        IPage<ApiPermissionVO> page = apiPermissionService.getUserApiPermissions(
                targetUser.getId(), pageNum, pageSize);

        return Result.success(page, "查询成功");
    }
}

