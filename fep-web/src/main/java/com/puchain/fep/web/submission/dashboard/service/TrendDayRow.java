package com.puchain.fep.web.submission.dashboard.service;

import com.puchain.fep.web.common.metrics.AggregateRows;
import java.util.Objects;

/**
 * Dashboard 趋势图按日聚合查询单行快照。
 *
 * <p>列序固定 {@code [date(yyyy-MM-dd), pushed, pending]}，与
 * {@code SubSubmissionRecordRepository.aggregateTrendByDate()} JPQL 列序一致（显式命名映射，
 * 消除 {@code Object[]} 裸索引消费）。pushed 仅 PUSHED、pending 仅 PENDING。</p>
 *
 * <p><strong>设计权衡：</strong>本 record 在 stream 内被即时消费后丢弃（非长生命周期值对象），
 * 取 record 而非裸命名常量是为统一 {@code RequestStateCountSnapshot} 既有 snapshot 模式 +
 * 让字段名文档化 JPQL 列序——JPQL 列重排时编译器立即在工厂处报错，优于魔数索引静默错位。</p>
 *
 * @param date    日期字符串（yyyy-MM-dd）
 * @param pushed  当日已推送数
 * @param pending 当日待推送数
 */
record TrendDayRow(String date, long pushed, long pending) {

    private static final int COL_DATE = 0;
    private static final int COL_PUSHED = 1;
    private static final int COL_PENDING = 2;

    /**
     * 由聚合查询单行结果构建快照。
     *
     * @param row 聚合查询单行 {@code Object[]}（3 列），非空
     * @return 快照
     */
    static TrendDayRow fromAggregateRow(final Object[] row) {
        Objects.requireNonNull(row, "row");
        return new TrendDayRow(
                (String) row[COL_DATE],
                AggregateRows.toLong(row[COL_PUSHED]),
                AggregateRows.toLong(row[COL_PENDING]));
    }
}
