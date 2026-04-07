package com.puchain.fep.web.sysmgmt.config.alert.domain;

/**
 * 预警通知方式枚举。
 *
 * <p>参见 PRD v1.3 §5.10.7.2d 接口预警管理（FR-WEB-SYS-CONF-ALERT）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public enum NotifyMethod {

    /** 邮件通知。 */
    EMAIL,

    /** 站内信通知。 */
    IN_APP,

    /** 短信通知。 */
    SMS
}
