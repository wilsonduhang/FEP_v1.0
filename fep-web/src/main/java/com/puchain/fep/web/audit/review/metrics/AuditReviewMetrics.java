package com.puchain.fep.web.audit.review.metrics;

import com.puchain.fep.web.common.metrics.CachedSupplier;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * §5.8 审核工作流 telemetry 门面（镜像 {@code CallbackMetrics}/{@code OutboundMetrics}，PRD §8.6）。
 *
 * <ul>
 *   <li>{@code fep_audit_review_decision_total{decision="APPROVED"|"REJECTED"}} — 审核终态决策计数</li>
 *   <li>{@code fep_audit_review_pending_count} — 当前待审核任务数 Gauge（审核队列积压观测）</li>
 *   <li>{@code fep_audit_review_failure_total{reason="not_found"|"terminal"|"lock_conflict"}}
 *       — 审核决策失败计数（客户端错误 / 并发冲突压力可观测）</li>
 * </ul>
 *
 * <p>本类仅 telemetry 门面，不涉状态机/DB。{@link MeterRegistry} 为 Spring Actuator
 * 自动装配的框架单例（同 {@code CallbackMetrics}，注入引用非外部可变状态）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class AuditReviewMetrics {

    static final String COUNTER_DECISION_TOTAL = "fep_audit_review_decision_total";
    static final String GAUGE_PENDING_COUNT = "fep_audit_review_pending_count";
    static final String COUNTER_FAILURE_TOTAL = "fep_audit_review_failure_total";
    private static final String TAG_DECISION = "decision";
    private static final String TAG_REASON = "reason";

    /** 审核任务不存在（{@code BIZ_5001}，HTTP 404）。 */
    public static final String REASON_NOT_FOUND = "not_found";
    /** 审核任务已终态，不可重复决策（{@code BIZ_5003}，HTTP 400）。 */
    public static final String REASON_TERMINAL = "terminal";
    /** 并发乐观锁冲突，任务已被他人处理（{@code BIZ_5003}，HTTP 400）。 */
    public static final String REASON_LOCK_CONFLICT = "lock_conflict";

    private final MeterRegistry registry;
    private final Clock clock;
    private final Duration countCacheTtl;

    /**
     * @param registry      Micrometer 注册中心（Actuator 自动装配 PrometheusMeterRegistry），非空
     * @param clock         时间来源（系统 {@link Clock} bean），非空
     * @param countCacheTtl pending count 缓存窗（{@code fep.metrics.count-cache-ttl}，默认 PT10S）
     */
    public AuditReviewMetrics(final MeterRegistry registry, final Clock clock,
            @Value("${fep.metrics.count-cache-ttl:PT10S}") final Duration countCacheTtl) {
        this.registry = Objects.requireNonNull(registry, "MeterRegistry must not be null");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.countCacheTtl = Objects.requireNonNull(countCacheTtl, "countCacheTtl");
    }

    /**
     * 记录一次审核终态决策（仅 APPROVED 终态 / REJECTED 调用，逐级推进不计）。
     *
     * @param decision {@link com.puchain.fep.web.audit.review.domain.ReviewStatus} name
     *                 （{@code APPROVED} 或 {@code REJECTED}），非空
     */
    public void recordDecision(final String decision) {
        registry.counter(COUNTER_DECISION_TOTAL, TAG_DECISION, decision).increment();
    }

    /**
     * 记录一次审核决策失败（按 {@code reason} 维度区分客户端错误 / 并发冲突压力）。
     *
     * @param reason 失败原因 tag（{@link #REASON_NOT_FOUND} / {@link #REASON_TERMINAL}
     *               / {@link #REASON_LOCK_CONFLICT}），非空
     */
    public void recordFailure(final String reason) {
        registry.counter(COUNTER_FAILURE_TOTAL, TAG_REASON, reason).increment();
    }

    /**
     * 注册待审核任务数 gauge（观测审核队列积压）。应在单例 service 初始化时注册一次。
     *
     * <p>供应函数经 {@link CachedSupplier} 在 TTL 窗内复用上次 {@code count(*)}，避免每次
     * Prometheus scrape 都打 DB（§8.6 一致化：与 {@code RequestStateMetrics} 共用同一缓存基元）。</p>
     *
     * @param pending 当前 PENDING 行数供应函数，非空
     */
    public void registerPendingGauge(final Supplier<Number> pending) {
        Gauge.builder(GAUGE_PENDING_COUNT, new CachedSupplier<>(pending, countCacheTtl, clock))
                .register(registry);
    }
}
