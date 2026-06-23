package com.puchain.fep.web.requeststate;

import com.puchain.fep.web.common.metrics.AggregateRows;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * {@link RequestStateMetrics} 单次聚合查询快照：一次
 * {@link RequestStateRepository#aggregateLifecycleAndBlockedCounts()} 结果，承载 5 个 lifecycle 状态
 * 计数 + correlation_blocked 计数，供 6 个 gauge 在同一 TTL 窗内共享读取（把每窗 6 次 {@code COUNT}
 * 往返压为 1 次，DEF-MC-1）。
 *
 * <p>不可变值对象；内部 {@code byStatus} EnumMap 不对外暴露（仅 {@link #countOf} / {@link #blocked}
 * 访问器），故无 SpotBugs {@code EI_EXPOSE_REP} 暴露面。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
final class RequestStateCountSnapshot {

    /**
     * 聚合查询固定列序（与 {@link RequestStateRepository#aggregateLifecycleAndBlockedCounts()} JPQL 前 5
     * 列逐一对应；第 6 列 blocked 在其后）。显式声明，enum 重排不致错位。
     */
    private static final RequestStateLifecycle[] COLUMN_ORDER = {
        RequestStateLifecycle.CREATED,
        RequestStateLifecycle.SENT,
        RequestStateLifecycle.RESULT_RECEIVED,
        RequestStateLifecycle.FAILED,
        RequestStateLifecycle.STUCK,
    };

    private final Map<RequestStateLifecycle, Long> byStatus;
    private final long blocked;

    private RequestStateCountSnapshot(final Map<RequestStateLifecycle, Long> byStatus,
            final long blocked) {
        this.byStatus = byStatus;
        this.blocked = blocked;
    }

    /**
     * 由聚合查询单行结果构建快照。<strong>列序固定</strong>
     * {@code [CREATED, SENT, RESULT_RECEIVED, FAILED, STUCK, blocked]}，与
     * {@link RequestStateRepository#aggregateLifecycleAndBlockedCounts()} JPQL 列序一致（显式命名映射，
     * enum 重排不致错位）。空表 {@code SUM} 返回 {@code null} → 归 0。
     *
     * @param row 聚合查询单行 {@code Object[]}（6 列，元素可能为 null），非空
     * @return 快照
     */
    static RequestStateCountSnapshot fromAggregateRow(final Object[] row) {
        Objects.requireNonNull(row, "row");
        final Map<RequestStateLifecycle, Long> byStatus =
                new EnumMap<>(RequestStateLifecycle.class);
        for (int i = 0; i < COLUMN_ORDER.length; i++) {
            byStatus.put(COLUMN_ORDER[i], AggregateRows.toLong(row[i]));
        }
        return new RequestStateCountSnapshot(byStatus,
                AggregateRows.toLong(row[COLUMN_ORDER.length]));
    }

    /**
     * @param status lifecycle 状态
     * @return 该状态行数（快照未含则 0）
     */
    long countOf(final RequestStateLifecycle status) {
        return byStatus.getOrDefault(status, 0L);
    }

    /**
     * @return correlation_blocked=true 行数（≥0）
     */
    long blocked() {
        return blocked;
    }
}
