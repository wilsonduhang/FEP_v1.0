package com.puchain.fep.web.audit.review.domain;

/**
 * 报文人工审核任务的状态（PRD v1.3 §5.8 多级审核 Phase2）。
 *
 * <p>状态机（单级，{@code fep.review.levels=1}）：
 * <pre>
 * PENDING  → APPROVED   (审核通过，currentLevel 达到 reviewLevel)
 * PENDING  → REJECTED   (任一层驳回)
 * APPROVED → (terminal)
 * REJECTED → (terminal)
 * </pre>
 * 多级（{@code levels>1}）时 PENDING 在 currentLevel 未达 reviewLevel 前自循环（仍 PENDING）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public enum ReviewStatus {

    /** 待审核（业务规则失败报文入队后的初始态）。 */
    PENDING,

    /** 审核通过（全部层级通过）。 */
    APPROVED,

    /** 审核驳回（任一层级驳回，附原因）。 */
    REJECTED
}
