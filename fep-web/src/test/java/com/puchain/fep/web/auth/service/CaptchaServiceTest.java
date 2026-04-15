package com.puchain.fep.web.auth.service;

import com.puchain.fep.web.auth.domain.CaptchaResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * CaptchaService 单元测试（使用 Mock StringRedisTemplate）。
 *
 * <p>通过 HashMap 模拟 Redis 存储，避免外部 Redis 依赖。</p>
 */
class CaptchaServiceTest {

    private CaptchaService service;
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOps;
    private final ConcurrentHashMap<String, String> store = new ConcurrentHashMap<>();

    @BeforeEach
    void setUp() {
        store.clear();

        @SuppressWarnings("unchecked")
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        valueOps = ops;

        // Mock set: store into HashMap
        doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            String value = invocation.getArgument(1);
            store.put(key, value);
            return null;
        }).when(valueOps).set(anyString(), anyString(), any(Duration.class));

        // Mock get: retrieve from HashMap
        when(valueOps.get(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            return store.get(key);
        });

        redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        // Mock delete: remove from HashMap
        when(redisTemplate.delete(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            return store.remove(key) != null;
        });

        service = new CaptchaService(redisTemplate);
    }

    @Test
    void generateShouldReturnNonEmptyCaptcha() {
        CaptchaResponse resp = service.generate();
        assertNotNull(resp.getCaptchaId());
        assertFalse(resp.getCaptchaId().isBlank());
        assertTrue(resp.getImageBase64().startsWith("data:image/png;base64,"));
        assertEquals(300L, resp.getTtlSeconds());
    }

    @Test
    void verifyAndConsumeShouldReturnTrueForCorrectCode() {
        CaptchaResponse resp = service.generate();
        String stored = store.get("fep:captcha:" + resp.getCaptchaId());
        assertNotNull(stored);
        assertTrue(service.verifyAndConsume(resp.getCaptchaId(), stored));
    }

    @Test
    void verifyAndConsumeShouldBeOneTimeUse() {
        CaptchaResponse resp = service.generate();
        String stored = store.get("fep:captcha:" + resp.getCaptchaId());
        assertTrue(service.verifyAndConsume(resp.getCaptchaId(), stored));
        // Second verification should fail (key deleted)
        assertFalse(service.verifyAndConsume(resp.getCaptchaId(), stored));
    }

    @Test
    void verifyAndConsumeWithWrongCodeShouldFail() {
        CaptchaResponse resp = service.generate();
        assertFalse(service.verifyAndConsume(resp.getCaptchaId(), "xxxx"));
        // Wrong input also consumes (prevents brute force)
        assertFalse(service.verifyAndConsume(resp.getCaptchaId(), "xxxx"));
    }

    @Test
    void verifyWithUnknownCaptchaIdShouldFail() {
        assertFalse(service.verifyAndConsume("unknown-id-123", "abcd"));
    }

    @Test
    void verifyWithNullParamsShouldReturnFalse() {
        assertFalse(service.verifyAndConsume(null, "xxxx"));
        assertFalse(service.verifyAndConsume("id", null));
    }

    /**
     * Bypass token 未配置（null）时，应回退到 Redis 路径，
     * 既有验证逻辑保持 100% 不变。
     */
    @Test
    void verifyAndConsumeWithNullBypassTokenFallsBackToRedis() {
        ReflectionTestUtils.setField(service, "bypassToken", null);
        store.put("fep:captcha:cid", "abcd");
        assertTrue(service.verifyAndConsume("cid", "abcd"));
        // 单次消费已生效：再次验证应失败（Redis 中已删除）
        assertFalse(service.verifyAndConsume("cid", "abcd"));
    }

    /**
     * Bypass token 已配置且用户输入与之匹配（忽略大小写）时，
     * 应直接返回 true 且 <strong>不</strong> 触达 Redis。
     */
    @Test
    void verifyAndConsumeWithBypassTokenMatchingSkipsRedis() {
        ReflectionTestUtils.setField(service, "bypassToken", "e2e-bypass");
        // 清除 setUp 阶段的 stubbing 调用，使 verifyNoInteractions 仅校验业务路径
        clearInvocations(redisTemplate, valueOps);
        assertTrue(service.verifyAndConsume("cid", "e2e-bypass"));
        // 大小写不敏感
        assertTrue(service.verifyAndConsume("cid", "E2E-BYPASS"));
        verifyNoInteractions(redisTemplate);
        verifyNoInteractions(valueOps);
    }

    /**
     * Bypass token 已配置但用户输入不匹配时，应回退到 Redis 路径。
     * 此处 Redis 中存有 "abcd"，用户输入也为 "abcd"（不等于 bypass token），
     * 故 fallback 校验通过。
     */
    @Test
    void verifyAndConsumeWithBypassTokenNonMatchingFallsBackToRedis() {
        ReflectionTestUtils.setField(service, "bypassToken", "e2e-bypass");
        store.put("fep:captcha:cid", "abcd");
        assertTrue(service.verifyAndConsume("cid", "abcd"));
        // 已消费
        verify(redisTemplate).delete(anyString());
    }
}
