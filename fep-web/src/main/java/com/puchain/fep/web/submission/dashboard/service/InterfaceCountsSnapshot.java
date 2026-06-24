package com.puchain.fep.web.submission.dashboard.service;

import com.puchain.fep.web.common.metrics.AggregateRows;
import java.util.Objects;

/**
 * Dashboard 接口计数聚合查询单行快照。
 *
 * <p>列序固定 {@code [total, enabled]}，与
 * {@code SubOutputInterfaceRepository.aggregateInterfaceCounts()} JPQL 列序一致（显式命名映射，
 * 消除 {@code Object[]} 裸索引消费，DEF-MC-REUSE-1 附带项）。空表 {@code SUM} 返回 {@code null} → 归 0。</p>
 *
 * @param total   接口总数
 * @param enabled 启用接口数
 */
record InterfaceCountsSnapshot(long total, long enabled) {

    private static final int COL_TOTAL = 0;
    private static final int COL_ENABLED = 1;

    /**
     * 由聚合查询单行结果构建快照。
     *
     * @param row 聚合查询单行 {@code Object[]}（2 列，元素可能为 null），非空
     * @return 快照
     */
    static InterfaceCountsSnapshot fromAggregateRow(final Object[] row) {
        Objects.requireNonNull(row, "row");
        return new InterfaceCountsSnapshot(
                AggregateRows.toLong(row[COL_TOTAL]),
                AggregateRows.toLong(row[COL_ENABLED]));
    }
}
