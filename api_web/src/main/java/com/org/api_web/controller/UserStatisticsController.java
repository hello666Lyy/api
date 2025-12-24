package com.org.api_web.controller;

import com.org.api_common.result.Result;
import com.org.api_common.vo.MyStatisticsVO;
import com.org.api_admin_service.service.StatisticsService;
import com.org.api_web.interceptor.JwtInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 用户统计控制器
 */
@RestController
@RequestMapping("/api/user")
public class UserStatisticsController {

    @Autowired
    private StatisticsService statisticsService;

    /**
     * 获取我的调用统计
     * 
     * @param startTime 开始时间（可选，格式：yyyy-MM-dd HH:mm:ss 或 yyyy-MM-dd）
     * @param endTime 结束时间（可选，格式：yyyy-MM-dd HH:mm:ss 或 yyyy-MM-dd）
     * @return 统计信息
     */
    @GetMapping("/myStatistics")
    public Result<MyStatisticsVO> getMyStatistics(
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime
    ) {
        // 1. 从Token中获取当前用户ID
        Long userId = JwtInterceptor.getCurrentUserId();
        if (userId == null) {
            throw new com.org.api_common.exception.BusinessException(
                    com.org.api_common.constant.ErrorCodeEnum.TOKEN_MISSING, "Token中未找到用户ID");
        }

        // 2. 解析时间参数
        LocalDateTime startDateTime = null;
        LocalDateTime endDateTime = null;
        
        if (startTime != null && !startTime.trim().isEmpty()) {
            try {
                if (startTime.length() == 10) {
                    // 格式：yyyy-MM-dd
                    startDateTime = java.time.LocalDate.parse(startTime).atStartOfDay();
                } else {
                    // 格式：yyyy-MM-dd HH:mm:ss
                    startDateTime = LocalDateTime.parse(startTime, 
                            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                }
            } catch (Exception e) {
                throw new com.org.api_common.exception.BusinessException(
                        com.org.api_common.constant.ErrorCodeEnum.PARAM_ERROR, "开始时间格式错误");
            }
        }
        
        if (endTime != null && !endTime.trim().isEmpty()) {
            try {
                if (endTime.length() == 10) {
                    // 格式：yyyy-MM-dd
                    endDateTime = java.time.LocalDate.parse(endTime).atTime(23, 59, 59);
                } else {
                    // 格式：yyyy-MM-dd HH:mm:ss
                    endDateTime = LocalDateTime.parse(endTime, 
                            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                }
            } catch (Exception e) {
                throw new com.org.api_common.exception.BusinessException(
                        com.org.api_common.constant.ErrorCodeEnum.PARAM_ERROR, "结束时间格式错误");
            }
        }

        // 3. 查询统计信息
        MyStatisticsVO statistics = statisticsService.getUserStatistics(userId, startDateTime, endDateTime);

        return Result.success(statistics, "查询成功");
    }
}

