package com.puchain.fep.web.submission.dashboard.service;

import com.puchain.fep.web.common.metrics.AggregateRows;
import java.util.Objects;

/**
 * Dashboard 推送状态计数聚合查询单行快照。
 *
 * <p>列序固定 {@code [total, pushed, pending]}，与
 * {@code SubSubmissionRecordRepository.aggregatePushStatusCounts()} JPQL 列序一致（显式命名映射，
 * 消除 {@code Object[]} 裸索引消费）。pending 仅 {@code PushStatus.PENDING}（与 JPQL 一致）。
 * 空表 {@code SUM} 返回 {@code null} → 归 0。</p>
 *
 * @param total   记录总数
 * @param pushed  已推送数
 * @param pending 待推送数（仅 PENDING）
 */
record PushStatusCountsSnapshot(long total, long pushed, long pending) {

    private static final int COL_TOTAL = 0;
    private static final int COL_PUSHED = 1;
    private static final int COL_PENDING = 2;

    /**
     * 由聚合查询单行结果构建快照。
     *
     * @param row 聚合查询单行 {@code Object[]}（3 列，元素可能为 null），非空
     * @return 快照
     */
    static PushStatusCountsSnapshot fromAggregateRow(final Object[] row) {
        Objects.requireNonNull(row, "row");
        return new PushStatusCountsSnapshot(
                AggregateRows.toLong(row[COL_TOTAL]),
                AggregateRows.toLong(row[COL_PUSHED]),
                AggregateRows.toLong(row[COL_PENDING]));
    }
}
