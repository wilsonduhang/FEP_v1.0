package com.puchain.fep.web.callback.domain;

/**
 * 回调队列状态常量（字符串非 enum，单写者 = CallbackQueueRunner，
 * 镜像 {@code OutboundMessageQueueEntity} 设计取舍）。
 *
 * @author FEP Team
 * @since 1.0.0
 */
public final class CallbackQueueStatus {

    /** 待发送。 */
    public static final String PENDING = "PENDING";
    /** 发送中（runner 已 claim，防双发，Phase 2 启用 lease）。 */
    public static final String SENDING = "SENDING";
    /** 推送成功（行内返 2xx）。 */
    public static final String DONE = "DONE";
    /** 推送失败（非 2xx / IO），P1 持久可见不自动重试，Phase 2 接管。 */
    public static final String FAILED = "FAILED";

    private CallbackQueueStatus() {
    }
}
