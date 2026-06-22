package com.puchain.fep.web.common.metrics;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 时间窗 memoize 的供应器：包裹一个昂贵的供应函数（典型 {@code SELECT COUNT(*)} 或单次聚合查询），
 * 在 TTL 窗内复用上次结果，避免每次 Prometheus scrape 都回查 DB。
 *
 * <p>用于 §8.6 可观测 gauge（{@code AuditReviewMetrics} 缓存 pending count /
 * {@code RequestStateMetrics} 缓存单次聚合快照——共享同一基元，保持「是否缓存」的设计一致）。
 * 泛型 {@code T} 为被缓存值类型（{@code Number} count 或聚合快照对象）。线程安全：gauge 在 scrape
 * 线程读取，{@link #get()} 全程 {@code synchronized}（低频，无竞争压力）。TTL=0 等价于不缓存
 * （每次刷新）。</p>
 *
 * @param <T> 被缓存的供应值类型
 * @author FEP Team
 * @since 1.0.0
 */
public final class CachedSupplier<T> implements Supplier<T> {

    private final Supplier<T> delegate;
    private final long ttlMillis;
    private final Clock clock;

    private T cached;
    private long lastRefreshEpochMilli;

    /**
     * @param delegate 实际供应函数（典型 {@code repository.count*()} 或单次聚合查询），非空
     * @param ttl      缓存有效窗（非空、非负；{@link Duration#ZERO} 表示不缓存）
     * @param clock    时间来源（生产注入系统 {@link Clock} bean，测试注入可控 Clock），非空
     */
    public CachedSupplier(final Supplier<T> delegate, final Duration ttl,
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
     * 返回缓存值；首次调用或 TTL 过期时回查 {@code delegate} 刷新。
     *
     * <p>约定：{@code delegate} 应返回非 null（典型 {@code repository.count*()} 恒返回装箱值）；
     * 若返回 null，则缓存失效、每次调用都重新回查 {@code delegate}。</p>
     *
     * @return 当前（可能为 TTL 窗内陈旧）值
     */
    @Override
    public synchronized T get() {
        final long now = clock.millis();
        if (cached == null || now - lastRefreshEpochMilli >= ttlMillis) {
            cached = delegate.get();
            lastRefreshEpochMilli = now;
        }
        return cached;
    }
}
