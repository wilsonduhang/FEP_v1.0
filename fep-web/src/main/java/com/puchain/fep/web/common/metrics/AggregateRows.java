package com.puchain.fep.web.common.metrics;

/**
 * 聚合查询结果行（{@code Object[]}）单元的安全数值转换工具。
 *
 * <p>JPA 聚合（{@code SUM} / {@code COUNT}）在空表或全 null 分组时返回 {@code null}，且不同
 * 方言下 {@code SUM} 可能返回 {@code Long} / {@code BigInteger} / {@code BigDecimal}；本工具统一
 * 经 {@link Number#longValue()} 归一，{@code null} 归 0。供 §8.6 可观测 gauge 聚合快照
 * （{@link com.puchain.fep.web.requeststate.RequestStateCountSnapshot}）与 dashboard 统计
 * （{@code SubDashboardService}）共享，消除逐字重复（DEF-MC-REUSE-1）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public final class AggregateRows {

    private AggregateRows() {
    }

    /**
     * 将聚合查询单元安全转 {@code long}。
     *
     * @param cell 聚合结果单元（可能为 {@code null}，或任意 {@link Number} 实现）
     * @return 非 null 的 long 值；{@code null} 转 0
     */
    public static long toLong(final Object cell) {
        return cell == null ? 0L : ((Number) cell).longValue();
    }
}
