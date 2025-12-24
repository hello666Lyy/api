package com.org.api_admin_service.service.serviceImpl;

import com.org.api_admin_service.service.ApiInfoService;
import com.org.api_admin_service.service.StatisticsService;
import com.org.api_common.constant.CacheKeyConstants;
import com.org.api_common.entity.ApiInfo;
import com.org.api_common.vo.GlobalStatisticsVO;
import com.org.api_common.vo.MyStatisticsVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 统计服务实现
 */
@Slf4j
@Service
public class StatisticsServiceImpl implements StatisticsService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ApiInfoService apiInfoService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public MyStatisticsVO getUserStatistics(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        MyStatisticsVO statistics = new MyStatisticsVO();
        
        // 如果没有指定时间范围，默认查询最近30天
        LocalDate startDate = startTime != null ? startTime.toLocalDate() : LocalDate.now().minusDays(29);
        LocalDate endDate = endTime != null ? endTime.toLocalDate() : LocalDate.now();
        
        long totalCalls = 0;
        long todayCalls = 0;
        LocalDate today = LocalDate.now();
        
        // 遍历日期范围，累加用户调用次数
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            String dateStr = currentDate.format(DATE_FORMATTER);
            
            // 查询用户当天的调用次数
            String userKey = CacheKeyConstants.API_METRICS_USER + ":" + userId + ":" + dateStr;
            String userCountStr = redisTemplate.opsForValue().get(userKey);
            long userCount = userCountStr != null ? Long.parseLong(userCountStr) : 0;
            
            totalCalls += userCount;
            
            // 如果是今天，记录今日调用次数
            if (currentDate.equals(today)) {
                todayCalls = userCount;
            }
            
            currentDate = currentDate.plusDays(1);
        }
        
        // 注意：由于Redis存储结构的限制，当前无法精确统计单个用户的成功/失败次数
        // Redis中存储的是按接口路径统计的全局成功/失败次数，不是按用户统计的
        // 这里使用估算方式：假设成功率为95%（可以根据实际情况调整）
        // 如果需要精确统计，需要修改ApiMetricsAspect的存储结构，增加按用户+状态的统计
        long successCalls = (long) (totalCalls * 0.95);
        long failedCalls = totalCalls - successCalls;
        
        // 如果总调用次数为0，成功和失败次数也应该是0
        if (totalCalls == 0) {
            successCalls = 0;
            failedCalls = 0;
        }
        
        statistics.setTotalCalls(totalCalls);
        statistics.setSuccessCalls(successCalls);
        statistics.setFailedCalls(failedCalls);
        statistics.setTodayCalls(todayCalls);
        
        // 查询各接口的调用统计（最近7天）
        List<MyStatisticsVO.ApiCallStat> apiCallStats = getApiCallStats(userId, LocalDate.now().minusDays(6), LocalDate.now());
        statistics.setApiCallStats(apiCallStats);
        
        return statistics;
    }
    
    @Override
    public GlobalStatisticsVO getGlobalStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        GlobalStatisticsVO result = new GlobalStatisticsVO();

        // 用户总数：此处不直接访问用户表，由上层（Controller）补充或后续扩展
        result.setTotalUsers(0L);

        // 2. 统计接口总数（api_info 表）
        java.util.List<ApiInfo> apiList = apiInfoService.list();
        long totalApis = apiList.stream().filter(api -> api.getStatus() != null && api.getStatus() == 1).count();
        result.setTotalApis(totalApis);

        // 3. 统计总调用次数和今日调用次数（基于 Redis 计数）
        LocalDate today = LocalDate.now();
        LocalDate startDate = startTime != null ? startTime.toLocalDate() : today.minusDays(29);
        LocalDate endDate = endTime != null ? endTime.toLocalDate() : today;

        long totalCalls = 0L;
        long todayCalls = 0L;
        long totalCostTime = 0L;

        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            String dateStr = currentDate.format(DATE_FORMATTER);

            long dayTotalCalls = 0L;
            long dayCostTime = 0L;

            // 累加所有接口当天的调用次数和耗时
            for (ApiInfo apiInfo : apiList) {
                if (apiInfo.getStatus() == null || apiInfo.getStatus() != 1) {
                    continue;
                }
                String apiPath = apiInfo.getApiPath();

                String totalKey = CacheKeyConstants.API_METRICS_TOTAL + ":" + apiPath + ":" + dateStr;
                String totalCountStr = redisTemplate.opsForValue().get(totalKey);
                long totalCount = totalCountStr != null ? Long.parseLong(totalCountStr) : 0L;
                dayTotalCalls += totalCount;

                // 累加响应时间
                String timeKey = CacheKeyConstants.API_METRICS_TIME + ":" + apiPath + ":" + dateStr;
                String timeStr = redisTemplate.opsForValue().get(timeKey);
                long cost = timeStr != null ? Long.parseLong(timeStr) : 0L;
                dayCostTime += cost;
            }

            totalCalls += dayTotalCalls;
            totalCostTime += dayCostTime;

            if (currentDate.equals(today)) {
                // 今日调用次数：使用当天所有接口的调用总和
                todayCalls = dayTotalCalls;
            }

            currentDate = currentDate.plusDays(1);
        }

        result.setTotalCalls(totalCalls);
        result.setTodayCalls(todayCalls);

        // 4. 计算平均响应时间（毫秒）
        if (totalCalls > 0 && totalCostTime > 0) {
            result.setAvgResponseTime(totalCostTime * 1.0 / totalCalls);
        } else {
            result.setAvgResponseTime(0.0);
        }

        return result;
    }
    
    /**
     * 获取各接口的调用统计
     */
    private List<MyStatisticsVO.ApiCallStat> getApiCallStats(Long userId, LocalDate startDate, LocalDate endDate) {
        List<MyStatisticsVO.ApiCallStat> stats = new ArrayList<>();
        
        // 获取所有已启用的接口
        List<ApiInfo> apiList = apiInfoService.list();
        
        // 遍历每个接口，统计调用次数
        for (ApiInfo apiInfo : apiList) {
            if (apiInfo.getStatus() != 1) {
                continue; // 跳过已禁用的接口
            }
            
            long callCount = 0;
            LocalDate currentDate = startDate;
            
            // 遍历日期范围，累加该接口的调用次数
            while (!currentDate.isAfter(endDate)) {
                String dateStr = currentDate.format(DATE_FORMATTER);
                String totalKey = CacheKeyConstants.API_METRICS_TOTAL + ":" + apiInfo.getApiPath() + ":" + dateStr;
                String countStr = redisTemplate.opsForValue().get(totalKey);
                if (countStr != null) {
                    callCount += Long.parseLong(countStr);
                }
                currentDate = currentDate.plusDays(1);
            }
            
            // 只返回有调用记录的接口
            if (callCount > 0) {
                MyStatisticsVO.ApiCallStat stat = new MyStatisticsVO.ApiCallStat();
                stat.setApiId(apiInfo.getId());
                stat.setApiName(apiInfo.getApiName());
                stat.setCallCount(callCount);
                stats.add(stat);
            }
        }
        
        // 按调用次数降序排序
        stats.sort((a, b) -> Long.compare(b.getCallCount(), a.getCallCount()));
        
        return stats;
    }
}

