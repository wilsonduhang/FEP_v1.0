package com.puchain.fep.web.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 测试用 Redis 配置 — 基于 ConcurrentHashMap 的 Mock StringRedisTemplate。
 *
 * <p>支持 opsForValue().set/get/increment、delete、hasKey、expire 操作，
 * 避免测试依赖外部 Redis 实例。TTL 在测试中被忽略（不过期）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Configuration
public class TestRedisConfiguration {

    /** 模拟 Redis 内存存储。 */
    private static final ConcurrentHashMap<String, String> STORE = new ConcurrentHashMap<>();

    /**
     * 获取底层存储（供集成测试直接读取验证码等值）。
     *
     * @return 内存存储 Map
     */
    public static ConcurrentHashMap<String, String> getStore() {
        return STORE;
    }

    /**
     * ConcurrentHashMap 支撑的 Mock StringRedisTemplate。
     *
     * <p>支持操作: set(key,value), set(key,value,Duration), get(key),
     * increment(key), delete(key), hasKey(key), expire(key,Duration)。</p>
     *
     * @return mock StringRedisTemplate
     */
    @Bean
    @Primary
    @SuppressWarnings("unchecked")
    public StringRedisTemplate stringRedisTemplate() {
        STORE.clear();

        StringRedisTemplate template = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(template.opsForValue()).thenReturn(valueOps);

        // set(key, value)
        doAnswer(inv -> {
            STORE.put(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(valueOps).set(anyString(), anyString());

        // set(key, value, Duration) — 存储，忽略 TTL
        doAnswer(inv -> {
            STORE.put(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(valueOps).set(anyString(), anyString(), any(Duration.class));

        // get(key)
        when(valueOps.get(anyString())).thenAnswer(inv -> STORE.get(inv.<String>getArgument(0)));

        // increment(key) — 模拟 INCR
        when(valueOps.increment(anyString())).thenAnswer(inv -> {
            String key = inv.getArgument(0);
            AtomicLong counter = new AtomicLong(0);
            String existing = STORE.get(key);
            if (existing != null) {
                try {
                    counter.set(Long.parseLong(existing));
                } catch (NumberFormatException ignored) {
                    /* treat non-numeric as 0 */
                }
            }
            long newVal = counter.incrementAndGet();
            STORE.put(key, String.valueOf(newVal));
            return newVal;
        });

        // delete(key)
        when(template.delete(anyString())).thenAnswer(inv -> {
            String key = inv.getArgument(0);
            return STORE.remove(key) != null;
        });

        // hasKey(key)
        when(template.hasKey(anyString())).thenAnswer(inv -> STORE.containsKey(inv.<String>getArgument(0)));

        // expire(key, Duration) — no-op
        when(template.expire(anyString(), any(Duration.class))).thenReturn(true);

        return template;
    }
}
