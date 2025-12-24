package com.org.api_web.controller;

import com.org.api_common.annotation.ApiMetrics;
import com.org.api_common.result.Result;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test")
public class RedisTestController {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    // 测试接口：往Redis存值+取值
    @GetMapping("/redis")
    public String testRedis() {
        stringRedisTemplate.opsForValue().set("test_key", "这是项目存入的测试值");
        String value = stringRedisTemplate.opsForValue().get("test_key");
        return "Redis连接结果：" + (value != null ? "成功！取出的值：" + value : "失败！");
    }

    /**
     * 测试异步统计接口 - 成功场景
     */
    @ApiMetrics(recordParams = true, recordResponse = true)
    @GetMapping("/metrics/success")
    public Result<String> testMetricsSuccess(@RequestParam(required = false) String accessKey) {
        return Result.success("测试成功，这条调用会被统计");
    }

    /**
     * 测试异步统计接口 - 失败场景
     */
    @ApiMetrics(recordParams = true, recordResponse = true)
    @GetMapping("/metrics/error")
    public Result<String> testMetricsError() {
        throw new RuntimeException("测试异常，这条调用会被统计为失败");
    }

    /**
     * 查看Redis队列中的数据
     */
    @GetMapping("/metrics/queue")
    public Result<Object> viewQueue() {
        String queueKey = "api:metrics:queue";
        Long queueSize = stringRedisTemplate.opsForList().size(queueKey);
        return Result.success("队列长度: " + queueSize);
    }

    /**
     * 查看实时统计
     */
    @GetMapping("/metrics/stats")
    public Result<Object> viewStats(@RequestParam String apiPath) {
        String today = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String totalKey = "api:metrics:total:" + apiPath + ":" + today;
        String successKey = "api:metrics:success:" + apiPath + ":" + today;
        String failKey = "api:metrics:fail:" + apiPath + ":" + today;

        String total = stringRedisTemplate.opsForValue().get(totalKey);
        String success = stringRedisTemplate.opsForValue().get(successKey);
        String fail = stringRedisTemplate.opsForValue().get(failKey);

        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("total", total != null ? total : "0");
        stats.put("success", success != null ? success : "0");
        stats.put("fail", fail != null ? fail : "0");
        stats.put("date", today);

        return Result.success(stats);
    }
}