package com.org.api_sdk;

import com.alibaba.fastjson2.JSON;
import com.org.api_common.result.Result;
import com.org.api_sdk.config.ApiConfig;
import com.org.api_sdk.exception.ApiException;
import com.org.api_sdk.service.ApiInfoService;
import com.org.api_sdk.service.UserService;
import com.org.api_sdk.service.BusinessApiService;
import com.org.api_sdk.util.NonceUtil;
import com.org.api_sdk.util.SignUtil;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * API客户端核心类
 * @author zhangzhenhui
 */
public class ApiClient {
    private static final Logger log = LoggerFactory.getLogger(ApiClient.class);

    private final ApiConfig config;
    private final OkHttpClient httpClient;
    private final UserService userService;
    private final ApiInfoService apiInfoService;
    private final BusinessApiService businessApiService;

    public ApiClient(ApiConfig config) {
        this.config = config;
        // 初始化HTTP客户端
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(config.getConnectTimeout(), TimeUnit.MILLISECONDS)
                .readTimeout(config.getReadTimeout(), TimeUnit.MILLISECONDS)
                .build();
        // 初始化服务类
        this.userService = new UserService(this);
        this.apiInfoService = new ApiInfoService(this);
        this.businessApiService = new BusinessApiService(this);
    }

    /**
     * 执行HTTP请求
     * @param method 请求方法（GET/POST/PUT/DELETE）
     * @param path 请求路径（如：/api/user/getUserInfo）
     * @param params 业务参数（不含签名参数，业务参数不参与签名计算）
     * @param responseClass 响应数据类型
     * @return 响应数据
     * 
     * 注意：签名验证只使用固定参数（accessKey、timestamp、nonce），业务参数不参与签名计算
     */
    public <T> T executeRequest(String method, String path, Map<String, Object> params, Class<T> responseClass) {
        try {
            // 1. 构建签名参数（只使用固定参数：accessKey、timestamp、nonce，业务参数不参与签名计算）
            long timestamp = System.currentTimeMillis() / 1000; // 秒级时间戳
            String nonce = NonceUtil.generateNonce(); // 使用SDK自己的工具类
            
            Map<String, Object> signParams = new HashMap<>();
            signParams.put("accessKey", config.getAccessKey());
            signParams.put("timestamp", timestamp);
            signParams.put("nonce", nonce);

            // 2. 生成签名（只使用固定参数，业务参数不参与签名计算）
            String sign = SignUtil.generateSign(signParams, config.getSecretKey());

            // 3. 构建完整请求参数（业务参数 + 签名参数 + sign）
            Map<String, Object> allParams = new HashMap<>();
            // 先添加业务参数
            if (params != null) {
                allParams.putAll(params);
            }
            // 再添加签名参数
            allParams.put("accessKey", config.getAccessKey());
            allParams.put("timestamp", timestamp);
            allParams.put("nonce", nonce);
            allParams.put("sign", sign);

            // 4. 构建请求URL
            String url = config.getBaseUrl() + path;

            // 5. 构建请求
            Request.Builder requestBuilder = new Request.Builder().url(url);

            // 6. 根据请求方法构建请求体
            if ("GET".equalsIgnoreCase(method)) {
                // GET请求：参数拼接到URL
                HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();
                for (Map.Entry<String, Object> entry : allParams.entrySet()) {
                    urlBuilder.addQueryParameter(entry.getKey(), String.valueOf(entry.getValue()));
                }
                requestBuilder.url(urlBuilder.build());
            } else {
                // POST/PUT/DELETE请求：使用form-data
                FormBody.Builder formBuilder = new FormBody.Builder();
                for (Map.Entry<String, Object> entry : allParams.entrySet()) {
                    formBuilder.add(entry.getKey(), String.valueOf(entry.getValue()));
                }
                requestBuilder.method(method, formBuilder.build());
            }

            Request request = requestBuilder.build();

            // 7. 打印请求日志（如果启用）
            if (config.isEnableLog()) {
                log.info("请求URL: {}", request.url());
                log.info("请求参数: {}", allParams);
            }

            // 8. 发送请求
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new ApiException(response.code(), "HTTP请求失败: " + response.message());
                }

                String responseBody = response.body() != null ? response.body().string() : null;
                if (responseBody == null || responseBody.isEmpty()) {
                    throw new ApiException("响应体为空");
                }

                // 9. 解析响应
                if (config.isEnableLog()) {
                    log.info("响应内容: {}", responseBody);
                }

                // 解析Result格式的响应（使用common中的Result类）
                Result result = JSON.parseObject(responseBody, Result.class);

                if (result.getCode() != 200) {
                    throw new ApiException(result.getCode(), result.getMsg());
                }

                // 解析data字段
                if (result.getData() == null) {
                    return null;
                }

                if (responseClass == null || responseClass == Void.class) {
                    return null;
                }

                // 将data转换为目标类型
                String dataJson = JSON.toJSONString(result.getData());
                return JSON.parseObject(dataJson, responseClass);
            }
        } catch (ApiException e) {
            throw e;
        } catch (IOException e) {
            throw new ApiException("网络请求异常: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ApiException("请求处理异常: " + e.getMessage(), e);
        }
    }

    /**
     * 获取用户服务
     */
    public UserService getUserService() {
        return userService;
    }

    /**
     * 获取接口信息服务
     */
    public ApiInfoService getApiInfoService() {
        return apiInfoService;
    }

    /**
     * 获取业务接口服务
     */
    public BusinessApiService getBusinessApiService() {
        return businessApiService;
    }

    /**
     * 获取配置
     */
    public ApiConfig getConfig() {
        return config;
    }
}