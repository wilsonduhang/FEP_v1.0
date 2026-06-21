package com.puchain.fep.web.common.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/** {@link CachedCountSupplier} 单测：TTL 窗内复用 / 窗外刷新 / 边界。 */
class CachedCountSupplierTest {

    private final AtomicReference<Instant> now =
            new AtomicReference<>(Instant.parse("2026-06-20T00:00:00Z"));

    /** 可前进的测试时钟（读 {@link #now}）。 */
    private final Clock clock = new Clock() {
        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(final ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now.get();
        }
    };

    private void advance(final Duration d) {
        now.set(now.get().plus(d));
    }

    @Test
    void firstCall_refreshesImmediately() {
        final AtomicInteger src = new AtomicInteger(7);
        final CachedCountSupplier cached =
                new CachedCountSupplier(src::get, Duration.ofSeconds(10), clock);

        assertThat(cached.get().longValue()).isEqualTo(7L);
    }

    @Test
    void withinTtl_reusesCachedValue() {
        final AtomicInteger src = new AtomicInteger(5);
        final CachedCountSupplier cached =
                new CachedCountSupplier(src::get, Duration.ofSeconds(10), clock);

        assertThat(cached.get().longValue()).isEqualTo(5L);   // 首读刷新 = 5
        src.set(2);
        advance(Duration.ofSeconds(9));                        // 仍在窗内
        assertThat(cached.get().longValue()).isEqualTo(5L);   // 复用陈旧值
    }

    @Test
    void afterTtl_refreshesValue() {
        final AtomicInteger src = new AtomicInteger(5);
        final CachedCountSupplier cached =
                new CachedCountSupplier(src::get, Duration.ofSeconds(10), clock);

        assertThat(cached.get().longValue()).isEqualTo(5L);
        src.set(2);
        advance(Duration.ofSeconds(10));                       // 达到 TTL 边界
        assertThat(cached.get().longValue()).isEqualTo(2L);   // 刷新
    }

    @Test
    void wellBeyondTtl_refreshesValue() {
        final AtomicInteger src = new AtomicInteger(5);
        final CachedCountSupplier cached =
                new CachedCountSupplier(src::get, Duration.ofSeconds(10), clock);

        assertThat(cached.get().longValue()).isEqualTo(5L);
        src.set(2);
        advance(Duration.ofSeconds(15));                       // 远超 TTL 窗
        assertThat(cached.get().longValue()).isEqualTo(2L);   // 仍刷新
    }

    @Test
    void zeroTtl_alwaysRefreshes() {
        final AtomicInteger src = new AtomicInteger(1);
        final CachedCountSupplier cached =
                new CachedCountSupplier(src::get, Duration.ZERO, clock);

        assertThat(cached.get().longValue()).isEqualTo(1L);
        src.set(3);
        assertThat(cached.get().longValue()).isEqualTo(3L);   // 0 窗 = 不缓存
    }

    @Test
    void negativeTtl_rejected() {
        assertThatThrownBy(() ->
                new CachedCountSupplier(() -> 0, Duration.ofSeconds(-1), clock))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nullArgs_rejected() {
        assertThatThrownBy(() -> new CachedCountSupplier(null, Duration.ofSeconds(1), clock))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CachedCountSupplier(() -> 0, null, clock))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CachedCountSupplier(() -> 0, Duration.ofSeconds(1), null))
                .isInstanceOf(NullPointerException.class);
    }
}
