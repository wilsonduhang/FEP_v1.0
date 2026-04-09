package com.puchain.fep.web.tlq.queue.domain;

/**
 * TLQ 通道类型枚举，定义实时通道与批量通道。
 *
 * <p>参见 PRD v1.3 §3.1.1 TLQ 通道规格：
 * <ul>
 *   <li>实时通道（REALTIME）：端口 20001，用于即时交互报文</li>
 *   <li>批量通道（BATCH）：端口 20002，用于批量数据报送</li>
 * </ul>
 * </p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public enum TlqChannelType {

    /** 实时通道，默认端口 20001。 */
    REALTIME,

    /** 批量通道，默认端口 20002。 */
    BATCH
}
