package com.puchain.fep.web.sysmgmt.config.alert.domain;

/**
 * 预警频率枚举。
 *
 * <p>参见 PRD v1.3 §5.10.7.2d 接口预警管理（FR-WEB-SYS-CONF-ALERT）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public enum AlertFrequency {

    /** 实时告警。 */
    REALTIME,

    /** 每小时汇总告警。 */
    HOURLY,

    /** 每日汇总告警。 */
    DAILY
}
