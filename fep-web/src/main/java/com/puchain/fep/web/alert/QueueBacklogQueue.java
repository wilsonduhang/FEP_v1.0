package com.puchain.fep.web.alert;

/**
 * 受积压监控的队列标识（DEF-B9-3）。
 *
 * <p>作为边沿触发状态的 key、{@link QueueBacklogEvent} 的队列字段、以及 yaml 各队列开关的映射。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public enum QueueBacklogQueue {

    /** 接口模式回调投递队列 {@code callback_queue}。 */
    CALLBACK,

    /** TLQ 出站报文队列 {@code outbound_message_queue}。 */
    OUTBOUND
}
