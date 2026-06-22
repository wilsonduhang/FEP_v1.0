package com.puchain.fep.web.requeststate;

import com.puchain.fep.web.common.metrics.CachedSupplier;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;

/**
 * 请求生命周期 telemetry：按状态 + correlation_blocked 计数 gauge（S2 request-state tracking，PRD §8.6）。
 *
 * <p>{@link MeterBinder} 实现，Spring Boot Actuator 自动对每个 {@code MeterBinder} bean 调用
 * {@link #bindTo(MeterRegistry)}（镜像 {@code CallbackMetrics} 的 telemetry 门面定位，但本类暴露的是
 * 由 DB 实时查询驱动的 gauge 而非 counter）。注册以下系列：</p>
 *
 * <ul>
 *   <li>{@code fep_request_state_count{status="CREATED|SENT|RESULT_RECEIVED|FAILED|STUCK"}}
 *       — 各生命周期状态当前行数 gauge；</li>
 *   <li>{@code fep_request_state_blocked_count} — {@code correlation_blocked=true} 行数 gauge。</li>
 * </ul>
 *
 * <p>6 个 gauge 的值均来自<strong>单次聚合查询</strong>
 * {@link RequestStateRepository#aggregateLifecycleAndBlockedCounts()}（5 lifecycle + blocked，
 * DEF-MC-1：把每次 scrape 的 6 次 {@code COUNT} 往返压为 1 次）。</p>
 *
 * <p><b>blocked 与 STUCK 区分</b>：结构性永等不到匹配的请求（见 {@link BlockedMessageTypes}）被 reaper
 * 排除、不计入 STUCK，但仍由独立 {@code blocked_count} gauge 可见，避免已知缺口噪声污染 STUCK 计数
 * （T4 review MAJOR CONCERN / 红线 {@code audit_maturity_label_needs_prd_trace}）。blocked 行同时计入
 * 其 lifecycle 桶与 blocked 列（二者正交）。</p>
 *
 * <p>聚合结果经 {@link com.puchain.fep.web.common.metrics.CachedSupplier} 在 TTL 窗内缓存为单个快照
 * {@link RequestStateCountSnapshot}，6 个 gauge 同窗共享读取，避免每次 Prometheus scrape 都打 DB
 * （§8.6 一致化：与 {@code AuditReviewMetrics} 共用同一缓存基元）。TTL 由
 * {@code fep.metrics.count-cache-ttl} 配置（默认 PT10S）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class RequestStateMetrics implements MeterBinder {

    static final String GAUGE_COUNT = "fep_request_state_count";
    static final String GAUGE_BLOCKED_COUNT = "fep_request_state_blocked_count";
    private static final String TAG_STATUS = "status";

    private final RequestStateRepository repository;
    private final Clock clock;
    private final Duration countCacheTtl;

    /**
     * Spring 构造器注入。
     *
     * @param repository    request_state JPA repository（非空）
     * @param clock         时间来源（系统 {@link Clock} bean），非空
     * @param countCacheTtl count 缓存窗（{@code fep.metrics.count-cache-ttl}，默认 PT10S）
     */
    public RequestStateMetrics(final RequestStateRepository repository, final Clock clock,
            @Value("${fep.metrics.count-cache-ttl:PT10S}") final Duration countCacheTtl) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.countCacheTtl = Objects.requireNonNull(countCacheTtl, "countCacheTtl");
    }

    /**
     * 向给定 registry 注册全部 request-state gauge。
     *
     * @param registry Micrometer 注册中心（Actuator 自动装配），非空
     */
    @Override
    public void bindTo(final MeterRegistry registry) {
        final CachedSupplier<RequestStateCountSnapshot> snapshot = new CachedSupplier<>(
                () -> RequestStateCountSnapshot.fromAggregateRow(
                        repository.aggregateLifecycleAndBlockedCounts().get(0)),
                countCacheTtl, clock);
        for (final RequestStateLifecycle status : RequestStateLifecycle.values()) {
            Gauge.builder(GAUGE_COUNT, () -> snapshot.get().countOf(status))
                    .tag(TAG_STATUS, status.name())
                    .description("Number of request_state rows in the given lifecycle status")
                    .register(registry);
        }
        Gauge.builder(GAUGE_BLOCKED_COUNT, () -> snapshot.get().blocked())
                .description("Number of request_state rows flagged correlation_blocked "
                        + "(excluded from STUCK detection)")
                .register(registry);
    }
}
