package com.puchain.fep.web.sysmgmt.message.domain;

/**
 * 消息类型枚举。
 *
 * <p>参见 PRD v1.3 §5.10.4 消息管理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public enum MessageType {

    /** 系统公告。 */
    SYSTEM_NOTICE,

    /** 业务提醒。 */
    BIZ_REMINDER,

    /** 告警通知。 */
    ALERT,

    /** 待办任务。 */
    TODO_TASK
}
