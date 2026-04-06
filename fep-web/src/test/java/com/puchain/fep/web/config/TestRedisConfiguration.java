package com.puchain.fep.web.config;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 测试用 Redis 配置 — 提供 Mock StringRedisTemplate。
 *
 * <p>避免测试依赖外部 Redis 实例。所有 Redis 操作返回空值。</p>
 */
@Configuration
public class TestRedisConfiguration {

    /**
     * Mock StringRedisTemplate（所有操作返回 null/false）。
     *
     * @return mock StringRedisTemplate
     */
    @Bean
    @Primary
    @SuppressWarnings("unchecked")
    public StringRedisTemplate stringRedisTemplate() {
        StringRedisTemplate mock = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(mock.opsForValue()).thenReturn(valueOps);
        when(mock.hasKey(anyString())).thenReturn(false);
        return mock;
    }
}
