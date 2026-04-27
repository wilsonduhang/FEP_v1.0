package com.puchain.fep.processor.reconciliation;

/**
 * 对账业务状态（PRD v1.3 §1991）。
 *
 * <p>枚举值集合严格对齐 V18 Flyway 迁移
 * {@code V18__create_reconciliation_tables.sql} 中的
 * {@code CONSTRAINT chk_recon_status CHECK (reconciliation_status IN
 * ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'DISCREPANCY'))}。任意新增/重命名
 * 必须同步更新 V18 SQL 与 {@link ReconciliationRecord#getStatus()} 调用点。</p>
 *
 * <p>典型生命周期：</p>
 * <pre>
 * PENDING ──► IN_PROGRESS ──► COMPLETED
 *                       └──► DISCREPANCY
 * </pre>
 *
 * <p>状态机不在本枚举内强制——
 * 由 {@code BankReconciliationService} / {@code PlatformReconciliationService}
 * 在写库前显式约束（参见 Task 4/5/6）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public enum ReconciliationStatus {

    /** 已登记，尚未启动对账（3116 落库初态 / 3107-3108 等待对端响应）。 */
    PENDING,

    /** 对账进行中（业务处理器开始比对计数 / 金额）。 */
    IN_PROGRESS,

    /** 对账成功（终态）：双方计数+金额一致，{@code discrepancyCount = 0}。 */
    COMPLETED,

    /** 对账存在差异（终态或可重对）：计数/金额不一致 OR 业务规则违例。 */
    DISCREPANCY;

    /**
     * 是否为终态（COMPLETED 或 DISCREPANCY）。
     *
     * <p>当前 4 态语义中 PENDING / IN_PROGRESS 为非终态，
     * 状态机可允许它们继续转移；COMPLETED / DISCREPANCY 为业务终态，
     * 一般不再转移（DISCREPANCY 重对场景需新建 reconciliation 记录）。</p>
     *
     * @return {@code true} 当前是终态
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == DISCREPANCY;
    }
}
