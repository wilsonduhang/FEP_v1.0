package com.puchain.fep.web.outbound.consumer;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * P5 outbound queue consumer 配置项 (immutable record).
 *
 * <p>绑定前缀: {@code fep.outbound.queue}。Spring Boot 3.x record + {@code @ConfigurationProperties}
 * 自 3.0 起原生支持，无需 {@code @ConstructorBinding}。本类原 POJO 形态在 B1 实施时触发
 * SpotBugs {@code EI_EXPOSE_REP / EI_EXPOSE_REP2} 警告（getter 暴露内部 {@link Retry} 引用 +
 * {@link OutboundQueueConsumer} constructor 引用），改 record 后 component 不可变，警告自动消除。</p>
 *
 * <p>Components:</p>
 * <ul>
 *   <li>{@link #batchSize()} — 单轮 poll 最多拉取的待发送条目数（默认 50，{@code batch-size}）</li>
 *   <li>{@link #pollIntervalMs()} — 两轮 poll 之间的最小等待间隔（默认 1000ms，{@code poll-interval-ms}）</li>
 *   <li>{@link #retry()} — 重试退避策略嵌套配置（默认见 {@link Retry}）</li>
 * </ul>
 *
 * <p>默认值通过 {@link DefaultValue} 注解逐 component 提供，{@code @DefaultValue Retry retry}
 * 无值参数表示当配置源未声明 {@code retry.*} 时由 Spring Boot 3.1+ 自动构造默认实例
 * （递归应用 {@link Retry} 各 component 的 {@code @DefaultValue}）。
 * {@code application.yml} 同时声明显式默认值作为双保险。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "fep.outbound.queue")
public record OutboundQueueProperties(
        @DefaultValue("50") int batchSize,
        @DefaultValue("1000") long pollIntervalMs,
        @DefaultValue Retry retry) {

    /**
     * 重试退避策略嵌套配置 (immutable record).
     *
     * <p>Components:</p>
     * <ul>
     *   <li>{@link #backoffMillis()} — 首次重试退避（默认 30000ms，{@code retry.backoff-millis}）</li>
     *   <li>{@link #maxBackoffMillis()} — 重试退避上限（默认 1800000ms = 30min，{@code retry.max-backoff-millis}）</li>
     *   <li>{@link #maxAttempts()} — 最大重试次数（默认 5，超过进入 DLQ，{@code retry.max-attempts}）</li>
     * </ul>
     */
    public record Retry(
            @DefaultValue("30000") long backoffMillis,
            @DefaultValue("1800000") long maxBackoffMillis,
            @DefaultValue("5") int maxAttempts) {
    }
}
