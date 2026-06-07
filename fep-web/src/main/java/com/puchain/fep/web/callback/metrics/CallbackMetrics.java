package com.puchain.fep.web.callback.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 接口模式回调推送 telemetry：Counter / Timer 门面（镜像 {@code OutboundMetrics}，PRD §8.6）。
 *
 * <ul>
 *   <li>{@code fep_callback_send_total{status="SENT"|"RETRY"|"DEAD_LETTER"}}</li>
 *   <li>{@code fep_callback_send_latency_seconds} — 推送延迟 Timer</li>
 * </ul>
 *
 * <p>percentile 依赖 {@code application.yml}
 * {@code management.metrics.distribution.percentiles.fep_callback_send_latency_seconds}。
 * 本类仅 telemetry 门面，不涉状态机/DB。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class CallbackMetrics {

    static final String COUNTER_SEND_TOTAL = "fep_callback_send_total";
    static final String TIMER_SEND_LATENCY = "fep_callback_send_latency_seconds";
    static final String COUNTER_CREDENTIAL_EXPIRED = "fep_callback_credential_expired_total";
    private static final String TAG_STATUS = "status";
    private static final String STATUS_SENT = "SENT";
    private static final String STATUS_RETRY = "RETRY";
    private static final String STATUS_DEAD_LETTER = "DEAD_LETTER";

    private final MeterRegistry registry;

    /**
     * @param registry Micrometer 注册中心（Actuator 自动装配 PrometheusMeterRegistry），非空
     */
    public CallbackMetrics(final MeterRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "MeterRegistry must not be null");
    }

    /**
     * 记录一次成功推送：SENT counter+1 + latency timer。
     *
     * @param latencyNanos 推送耗时（纳秒，{@code System.nanoTime()} 差值，≥0）
     */
    public void recordSent(final long latencyNanos) {
        registry.counter(COUNTER_SEND_TOTAL, TAG_STATUS, STATUS_SENT).increment();
        registry.timer(TIMER_SEND_LATENCY, TAG_STATUS, STATUS_SENT)
                .record(latencyNanos, TimeUnit.NANOSECONDS);
    }

    /** 记录一次失败转 RETRY。 */
    public void recordRetry() {
        registry.counter(COUNTER_SEND_TOTAL, TAG_STATUS, STATUS_RETRY).increment();
    }

    /** 记录一次失败转 DEAD_LETTER。 */
    public void recordDeadLetter() {
        registry.counter(COUNTER_SEND_TOTAL, TAG_STATUS, STATUS_DEAD_LETTER).increment();
    }

    /** 记录一次凭证过期拒用（解析期 now &gt; expires_at）。 */
    public void recordCredentialExpired() {
        registry.counter(COUNTER_CREDENTIAL_EXPIRED).increment();
    }
}
