package com.puchain.fep.web.outbound.consumer;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

/**
 * P5 T8 outbound 发送 telemetry：Counter / Timer 注册门面。
 *
 * <p>三类指标（PRD v1.3 §8.6 监控告警，FR-MSG-OUTBOUND-METRICS）：</p>
 * <ul>
 *   <li>{@code fep_outbound_send_total{status="SENT"}} — 发送成功次数（{@link #recordSent}）</li>
 *   <li>{@code fep_outbound_send_total{status="RETRY"}} — 发送失败转 RETRY 次数（{@link #recordRetry}）</li>
 *   <li>{@code fep_outbound_send_total{status="DEAD_LETTER"}} — 发送失败转 DLQ 次数（{@link #recordDeadLetter}）</li>
 *   <li>{@code fep_outbound_send_latency_seconds} — 发送延迟 Timer（{@link #recordSent} 同步记录）</li>
 * </ul>
 *
 * <p><b>Percentile 暴露策略</b>：依赖 {@code application.yml} 中的
 * {@code management.metrics.distribution.percentiles.fep_outbound_send_latency_seconds: 0.5,0.95,0.99}
 * 配置项让 Micrometer 在客户端计算 quantile 并以
 * {@code fep_outbound_send_latency_seconds{quantile="0.5|0.95|0.99"}} 行形式暴露给 Prometheus
 * （Plan §AC2）。客户端 percentile 不支持跨实例聚合，但 FEP 单机部署足以覆盖 PRD 要求。
 * 如未来需要跨实例聚合，可改用 {@code percentiles-histogram=true} 切换至 server-side 直方图。</p>
 *
 * <p><b>调用约定</b>：T9 装配阶段由 {@code OutboundQueueRunner} 在 send → SENT 时调用
 * {@link #recordSent}（latencyNanos 来自 {@link System#nanoTime()} 差值），retry 转移时调用
 * {@link #recordRetry}，DLQ 转移时调用 {@link #recordDeadLetter}。本类不涉及任何状态机或
 * 数据库写入逻辑——仅 telemetry 门面，方便 retry/DLQ 路径与 metrics 解耦演进。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class OutboundMetrics {

    /** 发送总次数 Counter 名称。 */
    static final String COUNTER_SEND_TOTAL = "fep_outbound_send_total";

    /** 发送延迟 Timer 名称。 */
    static final String TIMER_SEND_LATENCY = "fep_outbound_send_latency_seconds";

    /** {@code status} tag 名。 */
    private static final String TAG_STATUS = "status";

    /** {@code status="SENT"} tag 值。 */
    private static final String STATUS_SENT = "SENT";

    /** {@code status="RETRY"} tag 值。 */
    private static final String STATUS_RETRY = "RETRY";

    /** {@code status="DEAD_LETTER"} tag 值。 */
    private static final String STATUS_DEAD_LETTER = "DEAD_LETTER";

    private final MeterRegistry registry;

    /**
     * 构造 OutboundMetrics。
     *
     * @param registry Micrometer {@link MeterRegistry}（Spring Boot Actuator 自动装配
     *                 PrometheusMeterRegistry，不为 null）
     */
    public OutboundMetrics(final MeterRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "MeterRegistry must not be null");
    }

    /**
     * 记录一次成功发送：SENT counter+1 + latency timer 记录耗时。
     *
     * @param latencyNanos 发送耗时（纳秒，通常来自 {@code System.nanoTime()} 差值，必须 ≥ 0）
     */
    public void recordSent(final long latencyNanos) {
        registry.counter(COUNTER_SEND_TOTAL, TAG_STATUS, STATUS_SENT).increment();
        registry.timer(TIMER_SEND_LATENCY).record(latencyNanos, TimeUnit.NANOSECONDS);
    }

    /**
     * 记录一次发送失败转 RETRY：RETRY counter+1。
     *
     * <p>状态机转移由 {@code OutboundRetryHandler} 负责，本方法只负责 telemetry。</p>
     */
    public void recordRetry() {
        registry.counter(COUNTER_SEND_TOTAL, TAG_STATUS, STATUS_RETRY).increment();
    }

    /**
     * 记录一次发送失败转 DEAD_LETTER：DEAD_LETTER counter+1。
     *
     * <p>状态机转移由 {@code OutboundRetryHandler} 负责，本方法只负责 telemetry。</p>
     */
    public void recordDeadLetter() {
        registry.counter(COUNTER_SEND_TOTAL, TAG_STATUS, STATUS_DEAD_LETTER).increment();
    }
}
