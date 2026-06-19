package com.puchain.fep.web.audit.review.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Objects;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

/**
 * §5.8 审核工作流 telemetry 门面（镜像 {@code CallbackMetrics}/{@code OutboundMetrics}，PRD §8.6）。
 *
 * <ul>
 *   <li>{@code fep_audit_review_decision_total{decision="APPROVED"|"REJECTED"}} — 审核终态决策计数</li>
 *   <li>{@code fep_audit_review_pending_count} — 当前待审核任务数 Gauge（审核队列积压观测）</li>
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
    private static final String TAG_DECISION = "decision";

    private final MeterRegistry registry;

    /**
     * @param registry Micrometer 注册中心（Actuator 自动装配 PrometheusMeterRegistry），非空
     */
    public AuditReviewMetrics(final MeterRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "MeterRegistry must not be null");
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
     * 注册待审核任务数 gauge（观测审核队列积压）。应在单例 service 初始化时注册一次。
     *
     * @param pending 当前 PENDING 行数供应函数，非空
     */
    public void registerPendingGauge(final Supplier<Number> pending) {
        Gauge.builder(GAUGE_PENDING_COUNT, pending).register(registry);
    }
}
