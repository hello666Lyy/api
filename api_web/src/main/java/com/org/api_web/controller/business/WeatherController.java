package com.org.api_web.controller.business;

import com.org.api_common.annotation.ApiMetrics;
import com.org.api_common.result.Result;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 天气查询接口（测试业务接口）
 */
@RestController
@RequestMapping("/api/business/weather")
public class WeatherController {

    /**
     * 查询天气
     * @param city 城市名称
     * @return 天气信息
     */
    @ApiMetrics
    @GetMapping("/query")
    public Result<Map<String, Object>> queryWeather(@RequestParam String city) {
        // 模拟天气数据
        Map<String, Object> weatherData = new HashMap<>();
        weatherData.put("city", city);
        weatherData.put("temperature", "22°C");
        weatherData.put("weather", "晴天");
        weatherData.put("humidity", "60%");
        weatherData.put("windSpeed", "5km/h");
        weatherData.put("updateTime", java.time.LocalDateTime.now().toString());
        
        return Result.success(weatherData, "查询成功");
    }
}

