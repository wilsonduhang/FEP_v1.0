package com.puchain.fep.processor.reconciliation;

/**
 * 清算指令执行状态（PRD v1.3 §2004）。
 *
 * <p>枚举值集合严格对齐 V18 Flyway 迁移
 * {@code V18__create_reconciliation_tables.sql} 中的
 * {@code CONSTRAINT chk_clearing_status CHECK (instruction_status IN
 * ('PENDING', 'PROCESSING', 'SUCCESS', 'FAILED'))}。任意新增/重命名
 * 必须同步更新 V18 SQL 与 {@link ClearingInstructionRecord#getInstructionStatus()} 调用点。</p>
 *
 * <p>典型生命周期：</p>
 * <pre>
 * PENDING ──► PROCESSING ──► SUCCESS
 *                       └──► FAILED （携带 failureCause）
 * </pre>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public enum ClearingInstructionStatus {

    /** 已登记，等待银行处理（3115 落库初态）。 */
    PENDING,

    /** 银行处理中。 */
    PROCESSING,

    /** 银行处理成功（终态）。 */
    SUCCESS,

    /** 银行处理失败（终态），必须携带 {@code failureCause}。 */
    FAILED;

    /**
     * 是否为终态（SUCCESS 或 FAILED）。
     *
     * @return {@code true} 当前是终态
     */
    public boolean isTerminal() {
        return this == SUCCESS || this == FAILED;
    }
}
