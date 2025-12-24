package com.org.api_web.aspect;

import com.alibaba.fastjson2.JSON;
import com.org.api_common.annotation.ApiMetrics;
import com.org.api_common.constant.CacheKeyConstants;
import com.org.api_common.entity.ApiCallLog;
import com.org.api_common.entity.ApiInfo;
import com.org.api_common.entity.SysUser;
import com.org.api_common.result.Result;
import com.org.api_service.service.SysUserService;
import com.org.api_admin_service.service.ApiInfoService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * API指标监控切面
 * 使用AOP + Redis异步统计接口调用数据
 */
@Slf4j
@Aspect
@Component
public class ApiMetricsAspect {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private ApiInfoService apiInfoService;

    // 限流配置常量
    private static final int IP_RATE_LIMIT_PER_MINUTE = 60; // 每分钟最多60次

    /**
     * 检查IP限流
     * @param ip 客户端IP
     * @param apiPath 接口路径
     * @return true=超限需拦截，false=未超限可放行
     */
    private boolean isIpRateLimited(String ip, String apiPath) {
        // 构建Redis key：api:rate:ip:{ip}:{path}:{yyyyMMddHHmm}
        String minute = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
        String rateLimitKey = "api:rate:ip:" + ip + ":" + apiPath + ":" + minute;

        // 计数+1，并设置过期时间（首次创建时）
        Long count = redisTemplate.opsForValue().increment(rateLimitKey);
        if (count != null && count == 1) {
            redisTemplate.expire(rateLimitKey, Duration.ofMinutes(1));
        }

        // 超过限制返回true（需要拦截）
        return count != null && count > IP_RATE_LIMIT_PER_MINUTE;
    }

    // 异步任务线程池（固定10个线程）
    private static final Executor asyncExecutor = Executors.newFixedThreadPool(10, r -> {
        Thread t = new Thread(r, "api-metrics-async");
        t.setDaemon(true);
        return t;
    });

    /**
     * 定义切点：拦截所有标记了@ApiMetrics的方法
     */
    @Pointcut("@annotation(com.org.api_common.annotation.ApiMetrics)")
    public void apiMetricsPointcut() {}

    /**
     * 环绕通知：统计接口调用
     */
    @Around("apiMetricsPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        final long startTime = System.currentTimeMillis();
        HttpServletRequest request = getRequest();
        final String accessKey = request.getParameter("accessKey");

        // ========== 新增：IP限流检查（在最前面）==========
        final String clientIp = getClientIp(request);
        final String apiPath = request.getRequestURI();

        if (isIpRateLimited(clientIp, apiPath)) {
            log.warn("[ApiMetrics] IP限流拦截: ip={}, path={}", clientIp, apiPath);
            // 直接返回限流响应，不执行原始方法
            return Result.fail(429, "访问过于频繁，请稍后再试");
        }
        // ============================================

        // 获取方法信息
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        ApiMetrics annotation = signature.getMethod().getAnnotation(ApiMetrics.class);
        final String method = request.getMethod();

        // 日志1：进入AOP，记录基本请求信息
        log.info("[ApiMetrics] 请求进入切面: method={}, path={}, accessKey={}", method, apiPath, accessKey);

        // 获取用户ID和接口ID
        final Long userId = getUserIdSafe(accessKey);
        final Long apiId = getApiIdSafe(apiPath, method);
        log.info("[ApiMetrics] 解析用户和接口完成: userId={}, apiId={}", userId, apiId);

        // 获取请求参数（根据注解配置决定是否记录）
        Map<String, Object> requestParams = null;
        String requestParamsJson = null;
        if (annotation == null || annotation.recordParams()) {
            requestParams = getRequestParams(request);
            requestParamsJson = JSON.toJSONString(requestParams);
        }

        Object result = null;
        String errorMsg = null;
        final Integer[] statusHolder = {1}; // 默认成功
        String responseResultJson = null;
        final long[] costTimeHolder = {0}; // 初始化耗时

        try {
            // 执行目标方法
            result = joinPoint.proceed();

            // 记录响应结果（根据注解配置决定是否记录）
            if ((annotation == null || annotation.recordResponse()) && result != null) {
                responseResultJson = JSON.toJSONString(result);
                // 限制响应结果长度，避免数据过大
                if (responseResultJson != null && responseResultJson.length() > 5000) {
                    responseResultJson = responseResultJson.substring(0, 5000) + "...(truncated)";
                }
            }

        } catch (Throwable e) {
            // 调用失败
            statusHolder[0] = 0;
            errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.length() > 500) {
                errorMsg = errorMsg.substring(0, 500);
            }
            throw e; // 重新抛出异常
        } finally {
            // 计算耗时（必须在finally中计算，确保无论成功失败都记录）
            costTimeHolder[0] = System.currentTimeMillis() - startTime;
            log.info("[ApiMetrics] Controller 执行完成: method={}, path={}, status={}, costTime={}ms",
                    method, apiPath, statusHolder[0], costTimeHolder[0]);

            // 更新实时统计（传入实际的costTime）- 改为异步
            final int finalStatus = statusHolder[0];
            final String finalResponseResultJson = responseResultJson;
            final String finalErrorMsg = errorMsg;
            final String finalRequestParamsJson = requestParamsJson;
            final long finalCostTime = costTimeHolder[0];

            // 将所有需要在lambda中用到的外部变量都提前final化/包裹，避免effectively final警告/bug
            CompletableFuture.runAsync(() -> {
                try {
                    log.info("[ApiMetrics] 异步更新实时统计开始: path={}, userId={}, success={}, costTime={}ms",
                            apiPath, userId, finalStatus == 1, finalCostTime);
                    updateRealTimeStatistics(apiPath, userId, finalStatus == 1, finalCostTime);
                    log.info("[ApiMetrics] 异步更新实时统计完成: path={}, userId={}", apiPath, userId);
                } catch (Exception e) {
                    log.error("更新实时统计失败", e);
                }
            }, asyncExecutor);

            // 构建调用日志对象
            ApiCallLog callLog = new ApiCallLog();
            callLog.setUserId(userId);
        callLog.setAccessKey(accessKey);
            callLog.setApiId(apiId);
            callLog.setIp(clientIp);
            callLog.setRequestParams(finalRequestParamsJson);
            callLog.setResponseResult(finalResponseResultJson);
            callLog.setStatus(finalStatus);
            callLog.setErrorMsg(finalErrorMsg);
            callLog.setCostTime(finalCostTime);
            callLog.setCallTime(LocalDateTime.now());

            // 异步写入Redis队列（使用自定义线程池，不阻塞主流程）
            CompletableFuture.runAsync(() -> {
                try {
                    log.info("[ApiMetrics] 异步写入调用日志到Redis队列开始: path={}, userId={}, apiId={}, status={}",
                            apiPath, userId, apiId, finalStatus);
                    saveCallLogToRedis(callLog);
                    log.info("[ApiMetrics] 异步写入调用日志到Redis队列完成: path={}, userId={}", apiPath, userId);
                } catch (Exception e) {
                    log.error("保存调用日志到Redis失败", e);
                }
            }, asyncExecutor);
        }

        return result;
    }

    /**
     * 安全获取用户ID，避免effectively final问题
     */
    private Long getUserIdSafe(String accessKey) {
        if (accessKey == null) return null;
        try {
            SysUser user = sysUserService.getByAccessKey(accessKey);
            if (user != null) {
                return user.getId();
            }
        } catch (Exception e) {
            log.warn("获取用户信息失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 安全获取接口ID，避免effectively final问题
     */
    private Long getApiIdSafe(String apiPath, String method) {
        try {
            ApiInfo apiInfo = apiInfoService.getValidApiByPathAndMethod(apiPath, method);
            if (apiInfo != null) {
                return apiInfo.getId();
            }
        } catch (Exception e) {
            log.debug("获取接口信息失败: {}", e.getMessage());
            // 接口可能未注册，不影响主流程
        }
        return null;
    }

    /**
     * 获取HttpServletRequest
     */
    private HttpServletRequest getRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new IllegalStateException("无法获取Request对象");
        }
        return attributes.getRequest();
    }

    /**
     * 获取请求参数
     */
    private Map<String, Object> getRequestParams(HttpServletRequest request) {
        Map<String, Object> params = new HashMap<>();
        request.getParameterMap().forEach((key, values) -> {
            // 过滤敏感参数
            if (isSensitiveParam(key)) {
                params.put(key, "***");
            } else if (values != null && values.length > 0) {
                params.put(key, values.length == 1 ? values[0] : values);
            }
        });
        return params;
    }

    /**
     * 判断是否为敏感参数
     */
    private boolean isSensitiveParam(String key) {
        String lowerKey = key.toLowerCase();
        return lowerKey.contains("password") ||
                lowerKey.contains("secret") ||
                lowerKey.contains("token") ||
                lowerKey.contains("sk");
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 处理多个IP的情况（取第一个）
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 保存调用日志到Redis队列
     */
    private void saveCallLogToRedis(ApiCallLog callLog) {
        try {
            String logJson = JSON.toJSONString(callLog);
            String queueKey = CacheKeyConstants.API_METRICS_QUEUE;
            Long size = redisTemplate.opsForList().rightPush(queueKey, logJson);

            // 只在队列首次创建时设置过期时间（size == 1 表示刚创建）
            if (size != null && size == 1) {
                redisTemplate.expire(queueKey, java.time.Duration.ofDays(7));
            }

            log.debug("调用日志已加入Redis队列");
        } catch (Exception e) {
            log.error("保存调用日志到Redis失败", e);
        }
    }

    /**
     * 更新实时统计（使用Redis计数器）
     */
    private void updateRealTimeStatistics(String apiPath, Long userId, boolean success, long costTime) {
        try {
            String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            // 统计维度1：接口调用总次数
            String totalKey = CacheKeyConstants.API_METRICS_TOTAL + ":" + apiPath + ":" + today;
            Long totalCount = redisTemplate.opsForValue().increment(totalKey);
            if (totalCount != null && totalCount == 1) {
                redisTemplate.expire(totalKey, java.time.Duration.ofDays(30));
            }

            // 统计维度2：成功/失败次数
            String statusKey = success
                    ? CacheKeyConstants.API_METRICS_SUCCESS + ":" + apiPath + ":" + today
                    : CacheKeyConstants.API_METRICS_FAIL + ":" + apiPath + ":" + today;
            Long statusCount = redisTemplate.opsForValue().increment(statusKey);
            if (statusCount != null && statusCount == 1) {
                redisTemplate.expire(statusKey, java.time.Duration.ofDays(30));
            }

            // 统计维度3：用户调用次数
            if (userId != null) {
                String userKey = CacheKeyConstants.API_METRICS_USER + ":" + userId + ":" + today;
                Long userCount = redisTemplate.opsForValue().increment(userKey);
                if (userCount != null && userCount == 1) {
                    redisTemplate.expire(userKey, java.time.Duration.ofDays(30));
                }
            }

            // 统计维度4：累计响应时间（用于计算平均值）
            if (costTime > 0) {
                String timeKey = CacheKeyConstants.API_METRICS_TIME + ":" + apiPath + ":" + today;
                Boolean keyExists = redisTemplate.hasKey(timeKey);
                redisTemplate.opsForValue().increment(timeKey, costTime);
                // 只在 key 不存在时设置过期时间
                if (Boolean.FALSE.equals(keyExists)) {
                    redisTemplate.expire(timeKey, java.time.Duration.ofDays(30));
                }
            }

        } catch (Exception e) {
            log.error("更新实时统计失败", e);
        }
    }
}