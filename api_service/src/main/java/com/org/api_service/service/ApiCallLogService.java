package com.org.api_service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.org.api_common.entity.ApiCallLog;

/**
 * API调用日志服务接口
 */
public interface ApiCallLogService extends IService<ApiCallLog> {

    /**
     * 定时任务：批量消费Redis队列中的日志并保存到数据库
     */
    void consumeCallLogQueue();
}