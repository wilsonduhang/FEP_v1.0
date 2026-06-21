package com.puchain.fep.web.common.metrics;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 时间窗 memoize 的 count 供应器：包裹一个昂贵的 count 供应函数（典型 {@code SELECT COUNT(*)}），
 * 在 TTL 窗内复用上次结果，避免每次 Prometheus scrape 都回查 DB。
 *
 * <p>用于 §8.6 可观测 gauge（{@code AuditReviewMetrics} / {@code RequestStateMetrics} 共享同一基元，
 * 保持「是否缓存 count」的设计一致）。线程安全：gauge 在 scrape 线程读取，{@link #get()} 全程
 * {@code synchronized}（低频，无竞争压力）。TTL=0 等价于不缓存（每次刷新）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public final class CachedCountSupplier implements Supplier<Number> {

    private final Supplier<Number> delegate;
    private final long ttlMillis;
    private final Clock clock;

    private Number cached;
    private long lastRefreshEpochMilli;

    /**
     * @param delegate 实际 count 供应函数（典型 {@code repository.count*()}），非空
     * @param ttl      缓存有效窗（非空、非负；{@link Duration#ZERO} 表示不缓存）
     * @param clock    时间来源（生产注入系统 {@link Clock} bean，测试注入可控 Clock），非空
     */
    public CachedCountSupplier(final Supplier<Number> delegate, final Duration ttl,
            final Clock clock) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        Objects.requireNonNull(ttl, "ttl");
        if (ttl.isNegative()) {
            throw new IllegalArgumentException("ttl must not be negative: " + ttl);
        }
        this.ttlMillis = ttl.toMillis();
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * 返回缓存的 count；首次调用或 TTL 过期时回查 {@code delegate} 刷新。
     *
     * <p>约定：{@code delegate} 应返回非 null（典型 {@code repository.count*()} 恒返回基本型
     * 装箱值）；若返回 null，则缓存失效、每次调用都重新回查 {@code delegate}。</p>
     *
     * @return 当前（可能为 TTL 窗内陈旧）count
     */
    @Override
    public synchronized Number get() {
        final long now = clock.millis();
        if (cached == null || now - lastRefreshEpochMilli >= ttlMillis) {
            cached = delegate.get();
            lastRefreshEpochMilli = now;
        }
        return cached;
    }
}
