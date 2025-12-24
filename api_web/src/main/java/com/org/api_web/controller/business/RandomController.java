package com.org.api_web.controller.business;

import com.org.api_common.annotation.ApiMetrics;
import com.org.api_common.result.Result;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * 随机数生成接口（测试业务接口）
 */
@RestController
@RequestMapping("/api/business/random")
public class RandomController {

    /**
     * 生成随机数
     * @param min 最小值（可选，默认0）
     * @param max 最大值（可选，默认100）
     * @param count 生成数量（可选，默认1）
     * @return 随机数信息
     */
    @ApiMetrics
    @GetMapping("/generate")
    public Result<Map<String, Object>> generateRandom(
            @RequestParam(required = false) Integer min,
            @RequestParam(required = false) Integer max,
            @RequestParam(required = false) Integer count
    ) {
        if (min == null) min = 0;
        if (max == null) max = 100;
        if (count == null || count < 1) count = 1;
        if (count > 100) count = 100; // 限制最多100个
        
        Random random = new Random();
        int[] numbers = new int[count];
        for (int i = 0; i < count; i++) {
            numbers[i] = random.nextInt(max - min + 1) + min;
        }
        
        Map<String, Object> randomData = new HashMap<>();
        randomData.put("min", min);
        randomData.put("max", max);
        randomData.put("count", count);
        randomData.put("numbers", numbers);
        randomData.put("generateTime", java.time.LocalDateTime.now().toString());
        
        return Result.success(randomData, "生成成功");
    }
}

