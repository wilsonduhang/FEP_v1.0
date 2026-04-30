package com.puchain.fep.collector.adapter.jdbc;

import java.util.Objects;

/**
 * {@link JdbcCollectorAdapter} 不可变配置（Java record）。
 *
 * <p>覆盖 PRD v1.3 §2.2.2 数仓模式 + §2.1「JDBC 适配器」配置三要素：
 * <ul>
 *   <li>{@code dataSourceBeanName} — Spring 上下文中 {@code NamedParameterJdbcTemplate}
 *       Bean 名（多源支持的关键，由调用方注入 Map 路由）</li>
 *   <li>{@code sql} — 命名参数 SQL，必须含 {@code :cursor} + {@code :batch_size}
 *       两个命名占位符</li>
 *   <li>{@code cursorColumn} — 增量水位列名（如 {@code created_at} / {@code id}）</li>
 *   <li>{@code initialWatermark} — 首次运行水位字符串（默认 {@code 1970-01-01T00:00:00Z}），
 *       由 {@link #withDefaults} 工厂方法填充</li>
 *   <li>{@code adapterId} — 与 {@code fep.collector.adapters[*].id} 一致</li>
 *   <li>{@code payloadDataType} — 报文数据类型（如 {@code INVOICE_CONTRACT_3101}），
 *       下游 {@code PayloadAssembler} 路由依据</li>
 * </ul>
 *
 * <p>compact 构造函数对所有字段执行 {@link Objects#requireNonNull} 校验。
 *
 * @author FEP Team
 * @since 1.0.0
 *
 * @param dataSourceBeanName 数据源 bean 名（非 null）
 * @param sql                命名参数 SQL（非 null，必须含 :cursor + :batch_size）
 * @param cursorColumn       增量水位列名（非 null）
 * @param initialWatermark   首次运行水位字符串（非 null；推荐 ISO-8601）
 * @param adapterId          适配器 ID（非 null）
 * @param payloadDataType    报文数据类型（非 null）
 */
public record JdbcAdapterConfig(
        String dataSourceBeanName,
        String sql,
        String cursorColumn,
        String initialWatermark,
        String adapterId,
        String payloadDataType
) {

    /** 默认初始水位 — UTC epoch（1970-01-01T00:00:00Z）。 */
    public static final String DEFAULT_INITIAL_WATERMARK = "1970-01-01T00:00:00Z";

    /**
     * compact 构造函数 — null 校验所有引用字段。
     */
    public JdbcAdapterConfig {
        Objects.requireNonNull(dataSourceBeanName, "dataSourceBeanName");
        Objects.requireNonNull(sql, "sql");
        Objects.requireNonNull(cursorColumn, "cursorColumn");
        Objects.requireNonNull(initialWatermark, "initialWatermark");
        Objects.requireNonNull(adapterId, "adapterId");
        Objects.requireNonNull(payloadDataType, "payloadDataType");
    }

    /**
     * 工厂方法 — {@code initialWatermark} 不指定时使用 {@link #DEFAULT_INITIAL_WATERMARK}。
     *
     * @param dataSourceBeanName 数据源 bean 名（非 null）
     * @param sql                命名参数 SQL（非 null）
     * @param cursorColumn       增量水位列名（非 null）
     * @param adapterId          适配器 ID（非 null）
     * @param payloadDataType    报文数据类型（非 null）
     * @return 新 {@link JdbcAdapterConfig} 实例（{@code initialWatermark = DEFAULT_INITIAL_WATERMARK}）
     */
    public static JdbcAdapterConfig withDefaults(
            final String dataSourceBeanName,
            final String sql,
            final String cursorColumn,
            final String adapterId,
            final String payloadDataType) {
        return new JdbcAdapterConfig(
                dataSourceBeanName, sql, cursorColumn,
                DEFAULT_INITIAL_WATERMARK, adapterId, payloadDataType);
    }
}
