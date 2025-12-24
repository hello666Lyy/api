package com.org.api_web.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.org.api_common.entity.ApiCallLog;
import com.org.api_common.result.Result;
import com.org.api_service.service.ApiCallLogService;
import com.org.api_service.service.SysUserService;
import com.org.api_admin_service.service.ApiInfoService;
import com.org.api_common.entity.ApiInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员调用日志查询接口
 */
@RestController
@RequestMapping("/api/admin/call-log")
public class AdminCallLogController {

    @Autowired
    private ApiCallLogService apiCallLogService;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private ApiInfoService apiInfoService;

    /**
     * 分页查询调用日志（管理员）
     */
    @GetMapping("/page")
    public Result<Page<ApiCallLog>> pageCallLogs(
            @RequestParam String accessKey,
            @RequestParam String sign,
            @RequestParam Long timestamp,
            @RequestParam String nonce,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String apiPath,
            @RequestParam(required = false) String targetAccessKey,
            @RequestParam(required = false) Integer status
    ) {
        // 1. 管理员权限校验
        sysUserService.verifyAdminPermission(accessKey, sign, timestamp, nonce);

        // 2. 构造查询条件
        QueryWrapper<ApiCallLog> wrapper = new QueryWrapper<>();
        if (apiPath != null && !apiPath.trim().isEmpty()) {
            wrapper.like("api_path", apiPath.trim());
        }
        if (targetAccessKey != null && !targetAccessKey.trim().isEmpty()) {
            wrapper.eq("access_key", targetAccessKey.trim());
        }
        if (status != null) {
            wrapper.eq("status", status);
        }
        wrapper.orderByDesc("call_time");

        // 3. 分页查询
        Page<ApiCallLog> page = apiCallLogService.page(new Page<>(pageNum, pageSize), wrapper);

        // 4. 补充接口路径/方法信息（非持久化字段，仅用于前端展示）
        if (page.getRecords() != null && !page.getRecords().isEmpty()) {
            java.util.Set<Long> apiIds = page.getRecords().stream()
                    .map(ApiCallLog::getApiId)
                    .filter(java.util.Objects::nonNull)
                    .collect(java.util.stream.Collectors.toSet());
            if (!apiIds.isEmpty()) {
                java.util.List<ApiInfo> apiInfos = apiInfoService.listByIds(apiIds);
                java.util.Map<Long, ApiInfo> apiMap = apiInfos.stream()
                        .collect(java.util.stream.Collectors.toMap(ApiInfo::getId, a -> a));
                page.getRecords().forEach(log -> {
                    ApiInfo api = apiMap.get(log.getApiId());
                    if (api != null) {
                        log.setApiPath(api.getApiPath());
                        log.setMethod(api.getMethod());
                    }
                });
            }
        }

        return Result.success(page, "查询成功");
    }
}


