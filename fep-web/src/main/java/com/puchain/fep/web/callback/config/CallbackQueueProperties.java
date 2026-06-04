package com.puchain.fep.web.callback.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 接口模式回调队列消费配置（immutable record，镜像 {@code OutboundQueueProperties}）。
 *
 * <p>绑定前缀 {@code fep.callback}。{@code poll-interval-ms} 与
 * {@code CallbackQueueRunner} 的 {@code @Scheduled} 占位符
 * {@code ${fep.callback.poll-interval-ms:5000}} 双绑（注解读占位符，代码读本 record）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "fep.callback")
public record CallbackQueueProperties(
        @DefaultValue("50") int batchSize,
        @DefaultValue("5000") long pollIntervalMs,
        @DefaultValue Retry retry,
        @DefaultValue Reaper reaper) {

    /**
     * 重试退避策略嵌套配置（默认 base=30s / max=30min / maxAttempts=3）。
     *
     * @param backoffMillis    首次重试退避（{@code retry.backoff-millis}）
     * @param maxBackoffMillis 退避上限（{@code retry.max-backoff-millis}）
     * @param maxAttempts      全局兜底最大重试次数（per-interface {@code SubOutputInterface.retryCount&gt;0}
     *                         时优先；默认 3 对齐 PRD §5.5.2「重试次数默认 3 次」）
     */
    public record Retry(
            @DefaultValue("30000") long backoffMillis,
            @DefaultValue("1800000") long maxBackoffMillis,
            @DefaultValue("3") int maxAttempts) {
    }

    /**
     * 僵尸 SENDING 行回收 reaper 配置（T13；默认 enabled / 60s 扫描 / 300s 滞留窗）。
     *
     * <p>{@code enabled} 经 {@code CallbackStaleReaper} 的 {@code @ConditionalOnProperty} Bean
     * 级装配（disabled 时 Bean 不创建，避免 {@code @Scheduled} 空跑噪声）；{@code intervalMs}
     * 经 reaper 的 {@code @Scheduled} 占位符双绑（注解读占位符，本字段供文档与绑定可见性）；
     * {@code staleAfterSeconds} 由 reaper 代码读取计算阈值。</p>
     *
     * @param enabled           是否启用 reaper（{@code reaper.enabled}，默认 true）
     * @param intervalMs        {@code @Scheduled fixedDelay} 扫描间隔（{@code reaper.interval-ms}，默认 60s）
     * @param staleAfterSeconds SENDING 行 {@code claimedAt} 早于 {@code now-staleAfterSeconds} 视为僵尸
     *                          （{@code reaper.stale-after-seconds}，默认 300s）
     */
    public record Reaper(
            @DefaultValue("true") boolean enabled,
            @DefaultValue("60000") long intervalMs,
            @DefaultValue("300") long staleAfterSeconds) {
    }
}
