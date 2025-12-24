package com.org.api_web.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.org.api_admin_service.service.ApiInfoService;
import com.org.api_common.entity.ApiCallLog;
import com.org.api_common.entity.ApiInfo;
import com.org.api_common.exception.BusinessException;
import com.org.api_common.result.Result;
import com.org.api_common.constant.ErrorCodeEnum;
import com.org.api_service.service.ApiCallLogService;
import com.org.api_web.interceptor.JwtInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户调用日志查询接口
 */
@RestController
@RequestMapping("/api/user/call-log")
public class UserCallLogController {

    @Autowired
    private ApiCallLogService apiCallLogService;

    @Autowired
    private ApiInfoService apiInfoService;

    /**
     * 分页查询当前登录用户的调用日志
     */
    @GetMapping("/page")
    public Result<Page<ApiCallLog>> pageMyCallLogs(
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String apiPath,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime
    ) {
        Long userId = JwtInterceptor.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCodeEnum.TOKEN_MISSING, "Token中未找到用户ID");
        }

        QueryWrapper<ApiCallLog> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        if (apiPath != null && !apiPath.trim().isEmpty()) {
            wrapper.like("api_path", apiPath.trim());
        }
        if (status != null) {
            wrapper.eq("status", status);
        }
        LocalDateTime start = parseDateTime(startTime, true);
        LocalDateTime end = parseDateTime(endTime, false);
        if (start != null) {
            wrapper.ge("call_time", start);
        }
        if (end != null) {
            wrapper.le("call_time", end);
        }
        wrapper.orderByDesc("call_time");

        Page<ApiCallLog> page = apiCallLogService.page(new Page<>(pageNum, pageSize), wrapper);

        // 补充接口信息（路径、方法）
        if (page.getRecords() != null && !page.getRecords().isEmpty()) {
            Set<Long> apiIds = page.getRecords().stream()
                    .map(ApiCallLog::getApiId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            if (!apiIds.isEmpty()) {
                List<ApiInfo> apiInfos = apiInfoService.listByIds(apiIds);
                Map<Long, ApiInfo> apiMap = apiInfos.stream()
                        .collect(Collectors.toMap(ApiInfo::getId, a -> a));
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

    /**
     * 解析日期时间字符串，支持 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss
     */
    private LocalDateTime parseDateTime(String dateTimeStr, boolean isStart) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return null;
        }
        try {
            String trimmed = dateTimeStr.trim();
            if (trimmed.length() == 10) {
                // yyyy-MM-dd
                return isStart
                        ? java.time.LocalDate.parse(trimmed).atStartOfDay()
                        : java.time.LocalDate.parse(trimmed).atTime(23, 59, 59);
            }
            return LocalDateTime.parse(trimmed, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR, "时间格式错误，应为 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss");
        }
    }
}

















