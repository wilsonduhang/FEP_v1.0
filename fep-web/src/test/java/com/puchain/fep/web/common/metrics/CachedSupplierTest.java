package com.puchain.fep.web.common.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** {@link CachedSupplier} 单测：TTL 窗内复用 / 窗外刷新 / 边界。 */
class CachedSupplierTest {

    /** 可前进的测试时钟。 */
    private final MutableTestClock clock =
            new MutableTestClock(Instant.parse("2026-06-20T00:00:00Z"));

    @Test
    void firstCall_refreshesImmediately() {
        final AtomicInteger src = new AtomicInteger(7);
        final CachedSupplier<Number> cached =
                new CachedSupplier<>(src::get, Duration.ofSeconds(10), clock);

        assertThat(cached.get().longValue()).isEqualTo(7L);
    }

    @Test
    void withinTtl_reusesCachedValue() {
        final AtomicInteger src = new AtomicInteger(5);
        final CachedSupplier<Number> cached =
                new CachedSupplier<>(src::get, Duration.ofSeconds(10), clock);

        assertThat(cached.get().longValue()).isEqualTo(5L);   // 首读刷新 = 5
        src.set(2);
        clock.advance(Duration.ofSeconds(9));                        // 仍在窗内
        assertThat(cached.get().longValue()).isEqualTo(5L);   // 复用陈旧值
    }

    @Test
    void afterTtl_refreshesValue() {
        final AtomicInteger src = new AtomicInteger(5);
        final CachedSupplier<Number> cached =
                new CachedSupplier<>(src::get, Duration.ofSeconds(10), clock);

        assertThat(cached.get().longValue()).isEqualTo(5L);
        src.set(2);
        clock.advance(Duration.ofSeconds(10));                       // 达到 TTL 边界
        assertThat(cached.get().longValue()).isEqualTo(2L);   // 刷新
    }

    @Test
    void wellBeyondTtl_refreshesValue() {
        final AtomicInteger src = new AtomicInteger(5);
        final CachedSupplier<Number> cached =
                new CachedSupplier<>(src::get, Duration.ofSeconds(10), clock);

        assertThat(cached.get().longValue()).isEqualTo(5L);
        src.set(2);
        clock.advance(Duration.ofSeconds(15));                       // 远超 TTL 窗
        assertThat(cached.get().longValue()).isEqualTo(2L);   // 仍刷新
    }

    @Test
    void zeroTtl_alwaysRefreshes() {
        final AtomicInteger src = new AtomicInteger(1);
        final CachedSupplier<Number> cached =
                new CachedSupplier<>(src::get, Duration.ZERO, clock);

        assertThat(cached.get().longValue()).isEqualTo(1L);
        src.set(3);
        assertThat(cached.get().longValue()).isEqualTo(3L);   // 0 窗 = 不缓存
    }

    @Test
    void negativeTtl_rejected() {
        assertThatThrownBy(() ->
                new CachedSupplier<>(() -> 0, Duration.ofSeconds(-1), clock))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nullArgs_rejected() {
        assertThatThrownBy(() -> new CachedSupplier<>(null, Duration.ofSeconds(1), clock))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CachedSupplier<>(() -> 0, null, clock))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CachedSupplier<>(() -> 0, Duration.ofSeconds(1), null))
                .isInstanceOf(NullPointerException.class);
    }
}
