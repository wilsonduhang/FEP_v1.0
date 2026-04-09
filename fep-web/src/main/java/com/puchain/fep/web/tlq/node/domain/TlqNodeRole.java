package com.puchain.fep.web.tlq.node.domain;

/**
 * TLQ 节点角色枚举。
 *
 * <p>定义四种角色：主节点生产者、主节点热备、从节点消费者、从节点热备。
 * 参见 PRD v1.3 §5.7 TLQ 节点管理（FR-WEB-TLQ-NODE）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public enum TlqNodeRole {

    /** 主节点（生产者/发送端）。 */
    MASTER_PRODUCER,

    /** 主节点热备（备用生产者）。 */
    MASTER_STANDBY,

    /** 从节点（消费者/接收端）。 */
    SLAVE_CONSUMER,

    /** 从节点热备（备用消费者）。 */
    SLAVE_STANDBY
}
