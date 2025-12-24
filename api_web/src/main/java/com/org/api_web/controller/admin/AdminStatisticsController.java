package com.org.api_web.controller.admin;

import com.org.api_admin_service.service.StatisticsService;
import com.org.api_common.result.Result;
import com.org.api_common.vo.GlobalStatisticsVO;
import com.org.api_service.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 管理员全局统计控制器
 * 对应前端的 /api/admin/statistics/global 接口
 */
@RestController
@RequestMapping("/api/admin/statistics")
public class AdminStatisticsController {

    @Autowired
    private StatisticsService statisticsService;

    @Autowired
    private SysUserService sysUserService;

    /**
     * 获取全局调用统计
     * 管理员需要通过 AK/SK 签名校验
     */
    @GetMapping("/global")
    public Result<GlobalStatisticsVO> getGlobalStatistics(
            @RequestParam String accessKey,
            @RequestParam String sign,
            @RequestParam Long timestamp,
            @RequestParam String nonce,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime
    ) {
        // 1. 管理员权限校验（AK/SK 签名）
        sysUserService.verifyAdminPermission(accessKey, sign, timestamp, nonce);

        // 2. 解析时间参数（与 UserStatisticsController 逻辑保持一致）
        LocalDateTime startDateTime = null;
        LocalDateTime endDateTime = null;

        if (startTime != null && !startTime.trim().isEmpty()) {
            startDateTime = parseDateTime(startTime, true);
        }
        if (endTime != null && !endTime.trim().isEmpty()) {
            endDateTime = parseDateTime(endTime, false);
        }

        // 3. 查询全局统计信息
        GlobalStatisticsVO statistics = statisticsService.getGlobalStatistics(startDateTime, endDateTime);
        // 额外补充：用户总数（直接统计用户表）
        statistics.setTotalUsers(sysUserService.count());
        return Result.success(statistics, "查询成功");
    }

    private LocalDateTime parseDateTime(String value, boolean isStart) {
        try {
            if (value.length() == 10) {
                // yyyy-MM-dd
                if (isStart) {
                    return java.time.LocalDate.parse(value).atStartOfDay();
                } else {
                    return java.time.LocalDate.parse(value).atTime(23, 59, 59);
                }
            } else {
                // yyyy-MM-dd HH:mm:ss
                return LocalDateTime.parse(value,
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }
        } catch (Exception e) {
            // 非严格场景，解析失败时返回 null，由调用方使用默认时间范围
            return null;
        }
    }
}


