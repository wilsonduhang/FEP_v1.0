package com.puchain.fep.web.sysmgmt.config.pushinterface.domain;

/**
 * 推送接口推送方式枚举。
 *
 * <p>参见 PRD v1.3 §5.10.7.2c 推送接口管理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public enum PushMethod {

    /** 自动推送（系统定时/事件触发）。 */
    AUTO,

    /** 手动推送（由操作人员主动发起）。 */
    MANUAL
}
