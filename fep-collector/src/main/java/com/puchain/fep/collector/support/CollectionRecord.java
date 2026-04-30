package com.puchain.fep.collector.support;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * 采集到的单条业务记录（不可变值对象）。
 *
 * <p>由 {@link CollectorAdapter#collect} 产出，下游 {@code PayloadAssembler}
 * 据此构造 XML 报文。
 *
 * <p><b>不可变性保证：</b>
 * <ul>
 *   <li>所有字段 {@code final}，无 setter</li>
 *   <li>{@link #rawData} 在构造时执行 {@link Map#copyOf} 防御拷贝；getter 返回不可变 Map，
 *       外部无法通过引用共享或 getter 篡改内部状态</li>
 *   <li>类声明 {@code final}，禁止子类绕过校验</li>
 * </ul>
 *
 * <p><b>幂等键约束：</b>{@link #idempotencyKey} 长度必须为 32 位十六进制字符串
 * （由 {@link IdempotencyKeyGenerator#generate} 产出）。本类构造时仅校验长度，
 * 字符集合法性由生成器侧保证。
 *
 * @author FEP Team
 * @since 1.0.0
 */
public final class CollectionRecord {

    /** 幂等键固定长度：32 hex chars = 128 bit。 */
    private static final int IDEMPOTENCY_KEY_LENGTH = 32;

    private final String adapterId;
    private final String sourceRef;
    private final String payloadDataType;
    private final Map<String, Object> rawData;
    private final Instant collectedAt;
    private final String idempotencyKey;

    private CollectionRecord(final Builder builder) {
        this.adapterId = Objects.requireNonNull(builder.adapterId, "adapterId");
        this.sourceRef = Objects.requireNonNull(builder.sourceRef, "sourceRef");
        this.payloadDataType = Objects.requireNonNull(builder.payloadDataType, "payloadDataType");
        // rawData 已在 Builder.rawData() setter 中执行 Map.copyOf 防御拷贝（不可变）。
        this.rawData = Objects.requireNonNull(builder.rawData, "rawData");
        this.collectedAt = Objects.requireNonNull(builder.collectedAt, "collectedAt");
        final String key = Objects.requireNonNull(builder.idempotencyKey, "idempotencyKey");
        if (key.length() != IDEMPOTENCY_KEY_LENGTH) {
            throw new IllegalArgumentException(
                    "idempotencyKey length must be " + IDEMPOTENCY_KEY_LENGTH
                            + ", got " + key.length());
        }
        this.idempotencyKey = key;
    }

    /**
     * @return 来源适配器 ID（与 {@link CollectorAdapter#getId()} 一致）
     */
    public String getAdapterId() {
        return adapterId;
    }

    /**
     * @return 业务记录在源系统的引用（如 JDBC 主键、文件偏移、MQ messageId）
     */
    public String getSourceRef() {
        return sourceRef;
    }

    /**
     * @return 报文数据类型（如 {@code INVOICE_CONTRACT_3101}），用于 PayloadAssembler 路由
     */
    public String getPayloadDataType() {
        return payloadDataType;
    }

    /**
     * @return 原始字段数据，不可变 Map（内部经 {@link Map#copyOf} 防御 +
     *         {@link Collections#unmodifiableMap} 包装明示意图）
     */
    public Map<String, Object> getRawData() {
        // 双层保护：内部已是 Map.copyOf 不可变实例，再包一层 unmodifiableMap
        // 让 SpotBugs EI_EXPOSE_REP 正确识别（前者它不识别）。
        return Collections.unmodifiableMap(rawData);
    }

    /**
     * @return 采集时刻（UTC Instant，由 adapter 在取数完成时盖戳）
     */
    public Instant getCollectedAt() {
        return collectedAt;
    }

    /**
     * @return 业务幂等键，固定 32 位小写十六进制字符串
     */
    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    /**
     * 创建新 Builder。
     *
     * @return 新 Builder 实例（非 null）
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * {@link CollectionRecord} 流式构造器。
     *
     * <p>用法：{@code CollectionRecord.builder().adapterId(...).build();}
     */
    public static final class Builder {

        private String adapterId;
        private String sourceRef;
        private String payloadDataType;
        private Map<String, Object> rawData;
        private Instant collectedAt;
        private String idempotencyKey;

        private Builder() {
            // 仅由 CollectionRecord.builder() 创建。
        }

        /**
         * 设置 adapterId。
         *
         * @param value 来源适配器 ID
         * @return this
         */
        public Builder adapterId(final String value) {
            this.adapterId = value;
            return this;
        }

        /**
         * 设置 sourceRef。
         *
         * @param value 业务记录在源系统的引用
         * @return this
         */
        public Builder sourceRef(final String value) {
            this.sourceRef = value;
            return this;
        }

        /**
         * 设置 payloadDataType。
         *
         * @param value 报文数据类型
         * @return this
         */
        public Builder payloadDataType(final String value) {
            this.payloadDataType = value;
            return this;
        }

        /**
         * 设置 rawData（setter 内立即执行 {@link Map#copyOf} 防御拷贝，
         * 后续外部对原始 map 的修改不会影响内部状态）。
         *
         * @param value 原始字段数据（非 null；空 Map 允许）
         * @return this
         * @throws NullPointerException 当 {@code value} 为 null（由 {@link Map#copyOf} 抛）
         */
        public Builder rawData(final Map<String, Object> value) {
            this.rawData = value == null ? null : Map.copyOf(value);
            return this;
        }

        /**
         * 设置 collectedAt。
         *
         * @param value 采集时刻
         * @return this
         */
        public Builder collectedAt(final Instant value) {
            this.collectedAt = value;
            return this;
        }

        /**
         * 设置 idempotencyKey（构造时会校验长度=32）。
         *
         * @param value 业务幂等键
         * @return this
         */
        public Builder idempotencyKey(final String value) {
            this.idempotencyKey = value;
            return this;
        }

        /**
         * 构造不可变 {@link CollectionRecord}。
         *
         * @return 新 record 实例
         * @throws NullPointerException 任一必填字段为 null
         * @throws IllegalArgumentException {@code idempotencyKey} 长度不为 32
         */
        public CollectionRecord build() {
            return new CollectionRecord(this);
        }
    }
}
