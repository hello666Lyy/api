package com.org.api_web;

import com.org.api_service.service.ApiCallLogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 临时手动触发调用日志消费，用于排查队列未落库问题。
 */
@SpringBootTest
public class ApiCallLogConsumeTest {

    @Autowired
    private ApiCallLogService apiCallLogService;

    @Test
    public void consumeQueueOnce() {
        apiCallLogService.consumeCallLogQueue();
    }
}






































