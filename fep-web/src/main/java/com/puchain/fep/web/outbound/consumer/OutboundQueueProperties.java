package com.puchain.fep.web.outbound.consumer;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * P5 outbound queue consumer 配置项。
 *
 * <p>绑定前缀: {@code fep.outbound.queue}。
 * 字段：</p>
 * <ul>
 *   <li>{@code batch-size} — 单轮 poll 最多拉取的待发送条目数（默认 50）</li>
 *   <li>{@code poll-interval-ms} — 两轮 poll 之间的最小等待间隔（默认 1000ms）</li>
 *   <li>{@code retry.backoff-millis} — 首次重试延迟（默认 30s）</li>
 *   <li>{@code retry.max-backoff-millis} — 重试退避上限（默认 30min）</li>
 *   <li>{@code retry.max-attempts} — 最大重试次数（默认 5，超过进入 DLQ）</li>
 * </ul>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "fep.outbound.queue")
public class OutboundQueueProperties {

    /** 默认单轮 poll 拉取上限。 */
    private static final int DEFAULT_BATCH_SIZE = 50;

    /** 默认 poll 间隔（毫秒，1 秒）。 */
    private static final long DEFAULT_POLL_INTERVAL_MS = 1000L;

    /** 默认首次重试退避（毫秒，30 秒）。 */
    private static final long DEFAULT_RETRY_BACKOFF_MILLIS = 30_000L;

    /** 默认重试退避上限（毫秒，30 分钟）。 */
    private static final long DEFAULT_RETRY_MAX_BACKOFF_MILLIS = 30L * 60L * 1000L;

    /** 默认最大重试次数（超过进入 DLQ）。 */
    private static final int DEFAULT_RETRY_MAX_ATTEMPTS = 5;

    /** 单轮 poll 拉取条目数上限。 */
    private int batchSize = DEFAULT_BATCH_SIZE;

    /** 两轮 poll 之间的最小等待间隔（毫秒）。 */
    private long pollIntervalMs = DEFAULT_POLL_INTERVAL_MS;

    /** 重试退避策略子配置。 */
    private Retry retry = new Retry();

    /**
     * 重试退避策略嵌套配置。
     */
    public static class Retry {
        /** 首次重试退避（毫秒，默认 30 秒）。 */
        private long backoffMillis = DEFAULT_RETRY_BACKOFF_MILLIS;
        /** 重试退避上限（毫秒，默认 30 分钟）。 */
        private long maxBackoffMillis = DEFAULT_RETRY_MAX_BACKOFF_MILLIS;
        /** 最大重试次数（默认 5，超过进入 DLQ）。 */
        private int maxAttempts = DEFAULT_RETRY_MAX_ATTEMPTS;

        public long getBackoffMillis() {
            return backoffMillis;
        }

        public void setBackoffMillis(long backoffMillis) {
            this.backoffMillis = backoffMillis;
        }

        public long getMaxBackoffMillis() {
            return maxBackoffMillis;
        }

        public void setMaxBackoffMillis(long maxBackoffMillis) {
            this.maxBackoffMillis = maxBackoffMillis;
        }

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public long getPollIntervalMs() {
        return pollIntervalMs;
    }

    public void setPollIntervalMs(long pollIntervalMs) {
        this.pollIntervalMs = pollIntervalMs;
    }

    public Retry getRetry() {
        return retry;
    }

    public void setRetry(Retry retry) {
        this.retry = retry;
    }
}
