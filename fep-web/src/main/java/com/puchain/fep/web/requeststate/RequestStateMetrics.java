package com.puchain.fep.web.requeststate;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.stereotype.Component;

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
 *       — 各生命周期状态当前行数 gauge（{@link RequestStateRepository#countByLifecycleStatus}）；</li>
 *   <li>{@code fep_request_state_blocked_count} — {@code correlation_blocked=true} 行数 gauge
 *       （{@link RequestStateRepository#countByCorrelationBlockedTrue}）。</li>
 * </ul>
 *
 * <p><b>blocked 与 STUCK 区分</b>：结构性永等不到匹配的请求（见 {@link BlockedMessageTypes}）被 reaper
 * 排除、不计入 STUCK，但仍由独立 {@code blocked_count} gauge 可见，避免已知缺口噪声污染 STUCK 计数
 * （T4 review MAJOR CONCERN / 红线 {@code audit_maturity_label_needs_prd_trace}）。</p>
 *
 * <p>gauge 值在 Prometheus scrape 时实时回查 DB（{@code count(*)} 轻量聚合），故无需本类持有可变状态。</p>
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

    /**
     * Spring 构造器注入。
     *
     * @param repository request_state JPA repository（非空）
     */
    public RequestStateMetrics(final RequestStateRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    /**
     * 向给定 registry 注册全部 request-state gauge。
     *
     * @param registry Micrometer 注册中心（Actuator 自动装配），非空
     */
    @Override
    public void bindTo(final MeterRegistry registry) {
        for (final RequestStateLifecycle status : RequestStateLifecycle.values()) {
            Gauge.builder(GAUGE_COUNT, repository,
                            repo -> repo.countByLifecycleStatus(status))
                    .tag(TAG_STATUS, status.name())
                    .description("Number of request_state rows in the given lifecycle status")
                    .register(registry);
        }
        Gauge.builder(GAUGE_BLOCKED_COUNT, repository,
                        repo -> repo.countByCorrelationBlockedTrue())
                .description("Number of request_state rows flagged correlation_blocked "
                        + "(excluded from STUCK detection)")
                .register(registry);
    }
}
