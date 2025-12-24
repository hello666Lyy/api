package com.org.api_admin_service.service;

import com.org.api_common.vo.GlobalStatisticsVO;
import com.org.api_common.vo.MyStatisticsVO;

import java.time.LocalDateTime;

/**
 * 统计服务接口
 */
public interface StatisticsService {

    /**
     * 获取用户的调用统计
     * @param userId 用户ID
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @return 统计信息
     */
    MyStatisticsVO getUserStatistics(Long userId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 获取全局调用统计（管理员视角）
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @return 全局统计信息
     */
    GlobalStatisticsVO getGlobalStatistics(LocalDateTime startTime, LocalDateTime endTime);
}

