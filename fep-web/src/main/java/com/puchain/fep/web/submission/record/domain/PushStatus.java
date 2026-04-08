package com.puchain.fep.web.submission.record.domain;

/**
 * 报送记录推送状态枚举。
 *
 * <p>参见 PRD v1.3 §5.6.4 报文推送。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public enum PushStatus {

    /** 待推送。 */
    PENDING,

    /** 推送中。 */
    PUSHING,

    /** 已推送。 */
    PUSHED,

    /** 推送失败。 */
    FAILED
}
