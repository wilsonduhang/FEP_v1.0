package com.puchain.fep.web.sysmgmt.role.domain;

/**
 * 角色类型枚举。
 *
 * <p>参见 PRD v1.3 §5.10.2（系统角色/业务角色/自定义角色）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public enum RoleType {

    /** 系统内置角色（不可删除）。 */
    SYSTEM,

    /** 业务角色。 */
    BUSINESS,

    /** 自定义角色。 */
    CUSTOM
}
