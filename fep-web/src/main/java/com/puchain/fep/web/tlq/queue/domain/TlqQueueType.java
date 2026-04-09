package com.puchain.fep.web.tlq.queue.domain;

/**
 * TLQ 队列类型枚举，定义各类队列的用途。
 *
 * <p>参见 PRD v1.3 §3.1.2 队列命名规范：
 * <ul>
 *   <li>LOCAL  — 本地队列（QLOCAL），机构本地接收队列</li>
 *   <li>REMOTE — 远端队列（QREMOTE），指向 HNDEMP 中心节点的对端队列</li>
 *   <li>DEST   — 目标队列（QDEST），路由目标定义</li>
 *   <li>SEND   — 发送队列（QSEND），绑定 REMOTE 的发送通道</li>
 *   <li>DEAD   — 死信队列（QDEAD），存放无法投递的报文</li>
 * </ul>
 * </p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public enum TlqQueueType {

    /** 本地队列。 */
    LOCAL,

    /** 远端队列（指向 HNDEMP 中心节点）。 */
    REMOTE,

    /** 目标队列。 */
    DEST,

    /** 发送队列。 */
    SEND,

    /** 死信队列。 */
    DEAD
}
