package com.org.api_common.util;

import com.org.api_common.constant.*;
//import com.org.api_common.constant.SignConstants;
import com.org.api_common.exception.BusinessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 改用 ApplicationContextAware 获取Spring容器中的Bean，避免静态注入坑
 */
@Component // 必须加，让Spring扫描并初始化
public class NonceUtil implements ApplicationContextAware {

    // 全局Spring上下文
    private static ApplicationContext applicationContext;

    // 先定义对应的RedisTemplate成员变量
    private static StringRedisTemplate redisTemplate;

    // 添加set方法，参数类型为StringRedisTemplate
    public static void setRedisTemplate(StringRedisTemplate redisTemplate) {
        NonceUtil.redisTemplate = redisTemplate;
    }

    /**
     * Spring初始化时自动注入上下文
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        NonceUtil.applicationContext = applicationContext;
    }

    /**
     * 获取RedisTemplate（从Spring上下文拿，确保非null）
     */
    private static StringRedisTemplate getRedisTemplate() {
        if (applicationContext == null) {
            throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR, "Spring上下文未初始化");
        }
        return applicationContext.getBean(StringRedisTemplate.class);
    }

    /**
     * 生成唯一Nonce
     */
    public static String generateNonce() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 验证Nonce是否重复（核心修改：用getRedisTemplate()拿Bean）
     */
    public static boolean verifyNonce(String ak, String nonce) {
        if (ak == null || ak.isEmpty() || nonce == null || nonce.isEmpty()) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR, "AK/Nonce不能为空");
        }
        // 从Spring上下文获取RedisTemplate（确保非null）
        StringRedisTemplate redisTemplate = getRedisTemplate();
        String nonceKey = CacheKeyConstants.NONCE_PREFIX + ak + ":" + nonce;

        // 检查Nonce是否已存在
        if (redisTemplate.hasKey(nonceKey)) {
            return false; // 重复，验证失败
        }
        // 存入Redis，设置过期时间（比如5分钟，避免缓存堆积）
        redisTemplate.opsForValue().set(nonceKey, "1", SignConstants.NONCE_EXPIRE_SECOND, TimeUnit.SECONDS);
        return true; // 验证通过
    }
}