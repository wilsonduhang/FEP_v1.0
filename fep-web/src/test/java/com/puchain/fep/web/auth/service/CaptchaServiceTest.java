package com.puchain.fep.web.auth.service;

import com.puchain.fep.web.auth.domain.CaptchaResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

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
    private final ConcurrentHashMap<String, String> store = new ConcurrentHashMap<>();

    @BeforeEach
    void setUp() {
        store.clear();

        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);

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

        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
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
}
