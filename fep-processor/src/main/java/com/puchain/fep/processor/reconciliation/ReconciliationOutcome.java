package com.puchain.fep.processor.reconciliation;

import java.util.Objects;

/**
 * 对账计算结果（不可变）。
 *
 * <p>由 {@link ReconciliationDiffCalculator} 在内存计算后产出，作为
 * {@code BankReconciliationService} / {@code PlatformReconciliationService}
 * 落库前的中间值对象（VO）。本类型仅承载计算结论，不直接持久化——
 * service 在 build {@link ReconciliationRecord} 时引用本结果填充
 * {@code status} / {@code discrepancyCount} / {@code actualCount} 字段。</p>
 *
 * <p><b>不变式（compact constructor 强制）</b>：</p>
 * <ul>
 *   <li>{@code status} 非空</li>
 *   <li>{@code declaredCount}, {@code actualSize}, {@code discrepancyCount} ≥ 0</li>
 *   <li>{@link ReconciliationStatus#COMPLETED} ⇒ {@code discrepancyCount == 0}
 *       （单向守护，违反则抛 {@link IllegalStateException}）</li>
 * </ul>
 *
 * <p><b>v1b 设计说明</b>：不加反向"DISCREPANCY ⇒ discrepancy &gt; 0"守护。
 * {@link ReconciliationDiffCalculator#validateBusinessRule(java.util.List)}
 * 在 {@code qsInfoList} 为 null 或空时返回 {@code (0, 0, DISCREPANCY, 0)}，
 * 该场景语义为"未提交清算指令"——是合法的业务异常组合，
 * 不应被反向守护拦截。Plan v1a 一度提议加反向守护，v1b P1-B-new1 修订删除以避免
 * dead code 误导。</p>
 *
 * @param declaredCount    报文声明的预期计数（如 3116 中 totalTransactionCount）
 * @param actualSize       实际观察到的条目数（list.size 或 paired count）
 * @param status           对账结论
 * @param discrepancyCount 差异条目数（不一致计数 / 业务规则违例数）
 * @author FEP Team
 * @since 1.0.0
 */
public record ReconciliationOutcome(
        int declaredCount,
        int actualSize,
        ReconciliationStatus status,
        int discrepancyCount) {

    /**
     * Compact constructor 强制 4 项不变式。
     *
     * @throws NullPointerException     if {@code status} is null
     * @throws IllegalArgumentException if any count is negative
     * @throws IllegalStateException    if status is COMPLETED but discrepancyCount &gt; 0
     */
    public ReconciliationOutcome {
        Objects.requireNonNull(status, "status");
        if (declaredCount < 0) {
            throw new IllegalArgumentException("declaredCount < 0");
        }
        if (actualSize < 0) {
            throw new IllegalArgumentException("actualSize < 0");
        }
        if (discrepancyCount < 0) {
            throw new IllegalArgumentException("discrepancyCount < 0");
        }
        if (status == ReconciliationStatus.COMPLETED && discrepancyCount != 0) {
            throw new IllegalStateException("COMPLETED requires discrepancyCount=0");
        }
    }
}
