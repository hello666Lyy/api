package com.org.api_web.controller.business;

import com.org.api_common.annotation.ApiMetrics;
import com.org.api_common.result.Result;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 时间查询接口（测试业务接口）
 */
@RestController
@RequestMapping("/api/business/time")
public class TimeController {

    /**
     * 获取当前时间
     * @param timezone 时区（可选，默认Asia/Shanghai）
     * @return 时间信息
     */
    @ApiMetrics
    @GetMapping("/current")
    public Result<Map<String, Object>> getCurrentTime(@RequestParam(required = false) String timezone) {
        if (timezone == null || timezone.isEmpty()) {
            timezone = "Asia/Shanghai";
        }
        
        Map<String, Object> timeData = new HashMap<>();
        timeData.put("timezone", timezone);
        timeData.put("currentTime", java.time.LocalDateTime.now().toString());
        timeData.put("timestamp", System.currentTimeMillis());
        timeData.put("date", java.time.LocalDate.now().toString());
        timeData.put("time", java.time.LocalTime.now().toString());
        
        return Result.success(timeData, "查询成功");
    }
}

