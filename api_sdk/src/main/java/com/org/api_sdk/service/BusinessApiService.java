package com.org.api_sdk.service;

import com.org.api_sdk.ApiClient;

import java.util.HashMap;
import java.util.Map;

/**
 * 业务接口API服务
 */
public class BusinessApiService {
    private final ApiClient client;

    public BusinessApiService(ApiClient client) {
        this.client = client;
    }

    /**
     * 查询天气
     * @param city 城市名称
     * @return 天气信息
     */
    public Map<String, Object> queryWeather(String city) {
        Map<String, Object> params = new HashMap<>();
        params.put("city", city);
        return client.executeRequest("GET", "/api/business/weather/query", params, Map.class);
    }

    /**
     * 获取当前时间
     * @param timezone 时区（可选，默认Asia/Shanghai）
     * @return 时间信息
     */
    public Map<String, Object> getCurrentTime(String timezone) {
        Map<String, Object> params = new HashMap<>();
        if (timezone != null && !timezone.isEmpty()) {
            params.put("timezone", timezone);
        }
        return client.executeRequest("GET", "/api/business/time/current", params, Map.class);
    }

    /**
     * 生成随机数
     * @param min 最小值（可选，默认0）
     * @param max 最大值（可选，默认100）
     * @param count 生成数量（可选，默认1）
     * @return 随机数信息
     */
    public Map<String, Object> generateRandom(Integer min, Integer max, Integer count) {
        Map<String, Object> params = new HashMap<>();
        if (min != null) {
            params.put("min", min);
        }
        if (max != null) {
            params.put("max", max);
        }
        if (count != null) {
            params.put("count", count);
        }
        return client.executeRequest("GET", "/api/business/random/generate", params, Map.class);
    }

    /**
     * 通用业务接口调用方法
     * @param apiPath 接口路径（如：/api/business/weather/query）
     * @param method 请求方式（GET/POST/PUT/DELETE）
     * @param params 业务参数
     * @param responseClass 响应数据类型
     * @return 响应数据
     */
    public <T> T callBusinessApi(String apiPath, String method, Map<String, Object> params, Class<T> responseClass) {
        return client.executeRequest(method, apiPath, params, responseClass);
    }
}

