package com.puchain.fep.web.sysmgmt.user.domain;

/**
 * 用户状态枚举。
 *
 * <p>参见 PRD v1.3 §5.10.1（正常/锁定/禁用）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public enum UserStatus {

    /** 活跃/启用。 */
    ACTIVE,

    /** 连续登录失败触发的临时锁定。 */
    LOCKED,

    /** 管理员禁用。 */
    DISABLED
}
