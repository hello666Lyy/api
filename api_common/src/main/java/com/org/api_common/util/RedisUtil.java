package com.org.api_common.util;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

/**
 * Redis工具类
 * @author zhangzhenhui
 */
public class RedisUtil {

    private static StringRedisTemplate redisTemplate;

    // 初始化RedisTemplate（通过Spring注入）
    public static void setRedisTemplate(StringRedisTemplate redisTemplate) {
        RedisUtil.redisTemplate = redisTemplate;
    }

    /**
     * 设置缓存（带过期时间）
     * @param key 键
     * @param value 值
     * @param expire 过期时间（秒）
     */
    public static void set(String key, String value, long expire) {
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        ops.set(key, value, expire, TimeUnit.SECONDS);
    }

    /**
     * 获取缓存
     * @param key 键
     * @return 值
     */
    public static String get(String key) {
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        return ops.get(key);
    }

    /**
     * 判断缓存是否存在
     * @param key 键
     * @return 是否存在
     */
    public static Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    /**
     * 删除缓存
     * @param key 键
     */
    public static void delete(String key) {
        redisTemplate.delete(key);
    }
}
