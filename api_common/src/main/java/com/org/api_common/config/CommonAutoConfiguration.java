package com.org.api_common.config;

import com.org.api_common.util.NonceUtil;
import com.org.api_common.util.RedisUtil;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 公共模块自动配置
 * @author zhangzhenhui
 */
@Configuration
public class CommonAutoConfiguration {

    /**
     * 初始化Redis工具类
     */
    @Bean
    @ConditionalOnBean(StringRedisTemplate.class)
    public RedisUtil redisUtil(StringRedisTemplate stringRedisTemplate) {
        RedisUtil.setRedisTemplate(stringRedisTemplate);
        return new RedisUtil();
    }

    /**
     * 初始化Nonce工具类
     */
    @Bean
    @ConditionalOnBean(StringRedisTemplate.class)
    public NonceUtil nonceUtil(StringRedisTemplate stringRedisTemplate) {
        NonceUtil.setRedisTemplate(stringRedisTemplate);
        return new NonceUtil();
    }
}