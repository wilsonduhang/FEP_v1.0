package com.puchain.fep.web.alert;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 队列积压告警配置（DEF-B9-3），绑定前缀 {@code fep.alert.queue-backlog}。
 *
 * <p>{@code interval-ms} 经 {@code QueueBacklogMonitor} 的 {@code @Scheduled} 占位符
 * {@code ${fep.alert.queue-backlog.interval-ms:60000}} 双绑（注解读占位符，本字段供绑定可见性）。
 * {@code threshold} 由监控器代码读取（积压深度阈值，运维按部署调参，非业务规则故走 yaml 非 DB，
 * muzhou 2026-06-24 D2 决策）。{@code immutable record}，镜像 {@code CallbackQueueProperties} 风格。</p>
 *
 * @param enabled         功能总开关（监控器 Bean 级 {@code @ConditionalOnProperty}，默认 true）
 * @param intervalMs      {@code @Scheduled fixedDelay} 采样间隔毫秒（默认 60000）
 * @param threshold       积压深度阈值（默认 1000；上穿即告警，运维须按部署实际流量调参）
 * @param callbackEnabled 是否监控 {@code callback_queue}（默认 true）
 * @param outboundEnabled 是否监控 {@code outbound_message_queue}（默认 true）
 * @author FEP Team
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "fep.alert.queue-backlog")
public record QueueBacklogAlertProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("60000") long intervalMs,
        @DefaultValue("1000") long threshold,
        @DefaultValue("true") boolean callbackEnabled,
        @DefaultValue("true") boolean outboundEnabled) {
}
