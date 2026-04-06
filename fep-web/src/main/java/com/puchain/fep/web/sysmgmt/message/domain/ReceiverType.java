package com.puchain.fep.web.sysmgmt.message.domain;

/**
 * 接收者类型枚举。
 *
 * <p>决定消息可见范围：指定用户 / 指定角色 / 全体用户。
 * 参见 PRD v1.3 §5.10.4 消息管理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public enum ReceiverType {

    /** 指定用户。 */
    USER,

    /** 指定角色下所有用户。 */
    ROLE,

    /** 全体用户广播。 */
    ALL
}
