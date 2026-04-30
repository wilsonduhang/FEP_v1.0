package com.puchain.fep.collector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * fep-collector 数据采集层配置。
 *
 * <p>绑定前缀 {@code fep.collector.*} 的配置。覆盖：
 * <ul>
 *   <li>{@link #adapters} — 适配器声明列表（id / type / cron / enabled / sourceConfig / payloadDataType）</li>
 *   <li>{@link #retry} — 失败重试策略（最大次数 + 初始 backoff）</li>
 *   <li>{@link #batchSize} — 单次采集批次条数（默认 500）</li>
 *   <li>{@link #lockTtlMillis} — 调度互斥锁 TTL（默认 5 min）</li>
 *   <li>{@link #institutionCode} — 当前机构代码（必填，启动期由调度/组装层校验）</li>
 * </ul>
 *
 * <p><b>backoffMillis 必须声明为 long</b>：T8 失败重试退避算式为
 * {@code backoffMillis << shift}，若 {@code int} 域，shift=30 时 {@code 1000 << 30}
 * 约 10¹² 超 {@link Integer#MAX_VALUE} 变负数，{@code Math.min(neg, 24h)} 取负值导致
 * next_retry_at 错误。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "fep.collector")
public class CollectorProperties {

    /** 默认单批次采集条数。 */
    public static final int DEFAULT_BATCH_SIZE = 500;

    /** 默认调度互斥锁 TTL（毫秒）：5 分钟。 */
    public static final long DEFAULT_LOCK_TTL_MILLIS = 300_000L;

    /** 适配器声明列表。 */
    private List<Adapter> adapters = new ArrayList<>();

    /** 失败重试策略。 */
    private Retry retry = new Retry();

    /** 单次采集批次条数（默认 {@link #DEFAULT_BATCH_SIZE}）。 */
    private int batchSize = DEFAULT_BATCH_SIZE;

    /** 调度互斥锁 TTL（毫秒，默认 {@link #DEFAULT_LOCK_TTL_MILLIS}）。 */
    private long lockTtlMillis = DEFAULT_LOCK_TTL_MILLIS;

    /** 机构代码（必填）。装配阶段由调度/组装层校验非空。 */
    private String institutionCode;

    public List<Adapter> getAdapters() {
        return adapters;
    }

    public void setAdapters(final List<Adapter> adapters) {
        this.adapters = adapters;
    }

    public Retry getRetry() {
        return retry;
    }

    public void setRetry(final Retry retry) {
        this.retry = retry;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(final int batchSize) {
        this.batchSize = batchSize;
    }

    public long getLockTtlMillis() {
        return lockTtlMillis;
    }

    public void setLockTtlMillis(final long lockTtlMillis) {
        this.lockTtlMillis = lockTtlMillis;
    }

    public String getInstitutionCode() {
        return institutionCode;
    }

    public void setInstitutionCode(final String institutionCode) {
        this.institutionCode = institutionCode;
    }

    /**
     * 单个适配器声明。
     */
    public static class Adapter {

        /** 适配器逻辑 ID（在 fep.collector.adapters 内唯一）。 */
        private String id;

        /** 适配器类型（JDBC / FILE / MQ / ESB），由 T1a 枚举校验。 */
        private String type;

        /** Cron 表达式（Spring 6 字段格式），为空表示仅手动触发。 */
        private String cron;

        /** 是否启用。 */
        private boolean enabled = true;

        /**
         * 数据源专属配置（key 由具体适配器解析，例如 JDBC 的 url/username/sql）。
         * 使用 Map 解耦，避免 Properties 与各适配器实现耦合。
         */
        private Map<String, String> sourceConfig = Map.of();

        /** 报文数据类型（如 INVOICE_CONTRACT_3101），用于 PayloadAssembler 路由。 */
        private String payloadDataType;

        public String getId() {
            return id;
        }

        public void setId(final String id) {
            this.id = id;
        }

        public String getType() {
            return type;
        }

        public void setType(final String type) {
            this.type = type;
        }

        public String getCron() {
            return cron;
        }

        public void setCron(final String cron) {
            this.cron = cron;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(final boolean enabled) {
            this.enabled = enabled;
        }

        public Map<String, String> getSourceConfig() {
            return sourceConfig;
        }

        public void setSourceConfig(final Map<String, String> sourceConfig) {
            this.sourceConfig = sourceConfig;
        }

        public String getPayloadDataType() {
            return payloadDataType;
        }

        public void setPayloadDataType(final String payloadDataType) {
            this.payloadDataType = payloadDataType;
        }
    }

    /**
     * 失败重试策略。
     *
     * <p><b>backoffMillis 必须为 long：</b>T8 退避算式 {@code backoffMillis << shift}
     * 在 int 域 shift=30 时 1000<<30 ≈ 10¹² 超过 {@link Integer#MAX_VALUE} 变负数，
     * 后续 {@code Math.min(neg, 24h)} 会取到负值，导致下次重试时间错误。
     */
    public static class Retry {

        /** 默认最大重试次数。 */
        public static final int DEFAULT_MAX_ATTEMPTS = 3;

        /** 默认初始 backoff（毫秒）。 */
        public static final long DEFAULT_BACKOFF_MILLIS = 1_000L;

        /** 最大重试次数（默认 {@link #DEFAULT_MAX_ATTEMPTS}）。 */
        private int maxAttempts = DEFAULT_MAX_ATTEMPTS;

        /** 初始退避（毫秒，必须为 long，默认 {@link #DEFAULT_BACKOFF_MILLIS}）。 */
        private long backoffMillis = DEFAULT_BACKOFF_MILLIS;

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(final int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public long getBackoffMillis() {
            return backoffMillis;
        }

        public void setBackoffMillis(final long backoffMillis) {
            this.backoffMillis = backoffMillis;
        }
    }
}
