package com.puchain.fep.web.callback.credential.oauth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link OAuth2TokenCache} 单元测试 — 验证 put/get / per-entry TTL 过期 / invalidate。
 *
 * @author FEP Team
 * @since 1.0.0
 */
class OAuth2TokenCacheTest {

    private OAuth2TokenCache cache;

    @BeforeEach
    void setUp() {
        cache = new OAuth2TokenCache();
    }

    @Test
    void putAndGetReturnsToken() {
        cache.put("IF-001", "token-abc", Duration.ofSeconds(60));
        assertThat(cache.get("IF-001")).isEqualTo(Optional.of("token-abc"));
    }

    @Test
    void getReturnsEmptyForUnknownInterface() {
        assertThat(cache.get("IF-UNKNOWN")).isEmpty();
    }

    @Test
    void getReturnsEmptyAfterTtl() throws InterruptedException {
        cache.put("IF-001", "token-abc", Duration.ofMillis(100));
        Thread.sleep(200);
        assertThat(cache.get("IF-001")).isEmpty();
    }

    @Test
    void invalidateRemovesEntry() {
        cache.put("IF-001", "token", Duration.ofMinutes(5));
        cache.invalidate("IF-001");
        assertThat(cache.get("IF-001")).isEmpty();
    }

    @Test
    void perEntryTtlIsIndependent() throws InterruptedException {
        cache.put("IF-short", "short-tok", Duration.ofMillis(100));
        cache.put("IF-long", "long-tok", Duration.ofMinutes(5));
        Thread.sleep(200);
        assertThat(cache.get("IF-short")).isEmpty();
        assertThat(cache.get("IF-long")).isEqualTo(Optional.of("long-tok"));
    }
}
