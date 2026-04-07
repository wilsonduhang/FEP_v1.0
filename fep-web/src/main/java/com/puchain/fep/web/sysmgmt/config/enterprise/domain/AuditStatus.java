package com.puchain.fep.web.sysmgmt.config.enterprise.domain;

/**
 * 企业主体审核状态枚举。
 *
 * <p>PENDING — 待审核；APPROVED — 已通过；REJECTED — 已拒绝。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public enum AuditStatus {

    /** 待审核。 */
    PENDING,

    /** 已通过。 */
    APPROVED,

    /** 已拒绝。 */
    REJECTED
}
