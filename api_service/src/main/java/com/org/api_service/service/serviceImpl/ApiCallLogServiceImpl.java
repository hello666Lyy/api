package com.org.api_service.service.serviceImpl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.org.api_service.mapper.ApiCallLogMapper;
import com.org.api_service.service.ApiCallLogService;
import com.org.api_common.constant.CacheKeyConstants;
import com.org.api_common.entity.ApiCallLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * API调用日志服务实现
 * 负责从Redis队列消费日志并批量写入数据库
 */
@Slf4j
@Service
public class ApiCallLogServiceImpl extends ServiceImpl<ApiCallLogMapper, ApiCallLog>
        implements ApiCallLogService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    // 最大重试次数
    private static final int MAX_RETRY_COUNT = 3;
    // 每次批量处理的最大数量
    private static final int BATCH_SIZE = 100;

    /**
     * 定时任务：每5秒批量消费Redis队列中的日志
     */
    @Scheduled(fixedDelay = 5000)
    @Transactional(rollbackFor = Exception.class)
    public void consumeCallLogQueue() {
        String queueKey = CacheKeyConstants.API_METRICS_QUEUE;
        List<ApiCallLog> logList = new ArrayList<>();

        // 批量从队列左侧弹出（FIFO）
        for (int i = 0; i < BATCH_SIZE; i++) {
            String logJson = redisTemplate.opsForList().leftPop(queueKey);
            if (logJson == null) {
                break; // 队列为空
            }

            try {
                ApiCallLog callLog = JSON.parseObject(logJson, ApiCallLog.class);
                logList.add(callLog);
            } catch (Exception e) {
                log.error("解析调用日志失败: {}", logJson, e);
            }
        }

        // 批量插入数据库
        if (!logList.isEmpty()) {
            // 过滤掉 apiId 为 null 的记录（避免违反数据库约束）
            List<ApiCallLog> validLogList = logList.stream()
                    .filter(callLog -> {
                        if (callLog.getApiId() == null) {
                            log.debug("过滤掉 apiId 为 null 的调用日志，API可能未在数据库中注册: userId={}, ip={}, status={}", 
                                    callLog.getUserId(), callLog.getIp(), callLog.getStatus());
                            return false;
                        }
                        return true;
                    })
                    .collect(Collectors.toList());
            
            int filteredCount = logList.size() - validLogList.size();
            if (filteredCount > 0) {
                log.warn("本次过滤掉 {} 条 apiId 为 null 的调用日志（API可能未在数据库中注册）", filteredCount);
            }
            
            if (!validLogList.isEmpty()) {
                try {
                    this.saveBatch(validLogList);
                    log.debug("批量保存调用日志成功，数量: {}", validLogList.size());
                } catch (Exception e) {
                    log.error("批量保存调用日志失败，数量: {}", validLogList.size(), e);
                    // 失败后重新放回队列（避免数据丢失）
                    retrySaveToQueue(queueKey, validLogList, 0);
                }
            }
        }
    }

    /**
     * 重试保存到队列
     */
    private void retrySaveToQueue(String queueKey, List<ApiCallLog> logList, int retryCount) {
        if (retryCount >= MAX_RETRY_COUNT) {
            log.error("重试次数已达上限，丢弃日志，数量: {}", logList.size());
            return;
        }

        try {
            for (ApiCallLog log : logList) {
                try {
                    String logJson = JSON.toJSONString(log);
                    redisTemplate.opsForList().rightPush(queueKey, logJson);
                } catch (Exception e) {
                    ApiCallLogServiceImpl.log.error("重新放回队列失败", e);
                }
            }
            log.warn("日志已重新放回队列，数量: {}, 重试次数: {}", logList.size(), retryCount + 1);
        } catch (Exception e) {
            log.error("重试保存到队列失败，重试次数: {}", retryCount, e);
            // 递归重试
            retrySaveToQueue(queueKey, logList, retryCount + 1);
        }
    }
}