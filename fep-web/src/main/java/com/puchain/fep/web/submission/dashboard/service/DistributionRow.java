package com.puchain.fep.web.submission.dashboard.service;

import com.puchain.fep.web.common.metrics.AggregateRows;
import java.util.Objects;

/**
 * Dashboard 分布 Top-N 聚合查询单行快照。
 *
 * <p>列序固定 {@code [name, value]}，与
 * {@code SubSubmissionRecordRepository.aggregateDistributionByMessageType()} /
 * {@code aggregateDistributionByBusinessType()} JPQL 列序一致（显式命名映射，
 * 消除 {@code Object[]} 裸索引消费）。businessType 维度下 {@code null} 业务类型已在 JPQL
 * 经 {@code COALESCE} 映射为 {@code "UNSPECIFIED"}。</p>
 *
 * <p><strong>设计权衡：</strong>同 {@link TrendDayRow}——stream 内即时消费，取 record 为统一
 * snapshot 模式 + 字段名文档化 JPQL 列序（列重排编译期报错优于魔数索引静默错位）。</p>
 *
 * @param name  维度名（messageType 或 businessTypeId）
 * @param value 计数
 */
record DistributionRow(String name, long value) {

    private static final int COL_NAME = 0;
    private static final int COL_VALUE = 1;

    /**
     * 由聚合查询单行结果构建快照。
     *
     * @param row 聚合查询单行 {@code Object[]}（2 列），非空
     * @return 快照
     */
    static DistributionRow fromAggregateRow(final Object[] row) {
        Objects.requireNonNull(row, "row");
        return new DistributionRow(
                (String) row[COL_NAME],
                AggregateRows.toLong(row[COL_VALUE]));
    }
}
