package com.puchain.fep.web.sysmgmt.message.domain;

/**
 * 消息状态枚举（逻辑删除标志）。
 *
 * <p>DELETED 表示逻辑删除，数据库不物理删除记录。
 * 参见 PRD v1.3 §5.10.4 消息管理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public enum MessageStatus {

    /** 正常状态，可见。 */
    NORMAL,

    /** 逻辑删除，不可见。 */
    DELETED
}
