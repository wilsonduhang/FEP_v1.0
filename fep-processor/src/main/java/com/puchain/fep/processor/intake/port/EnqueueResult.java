package com.puchain.fep.processor.intake.port;

import java.util.Objects;

/**
 * 出站入队结果（不可变 record，intake.port 跨模块契约）。
 *
 * <p>由 {@link OutboundMessageEnqueuePort#submit(OutboundMessageEnvelope)} 返回，
 * 标识本次入队最终落到 DB 的状态。{@link #status} = {@link Status#DUPLICATE}
 * 时 {@link #queueId} 仍为有效的已存在行 ID（便于审计追溯到首次入队）。
 *
 * @param queueId 持久化行 ID（{@code outbound_message_queue.queue_id}），非 null
 * @param status  入队状态，非 null
 * @author FEP Team
 * @since 1.0.0
 */
public record EnqueueResult(String queueId, Status status) {

    /**
     * compact 构造函数 — null 校验。
     */
    public EnqueueResult {
        Objects.requireNonNull(queueId, "queueId");
        Objects.requireNonNull(status, "status");
    }

    /**
     * 入队状态枚举。
     */
    public enum Status {

        /** 首次入队，DB 行新建。 */
        ENQUEUED,

        /** 同 idempotencyKey 已存在，本次未新增 DB 行（at-least-once 兜底）。 */
        DUPLICATE
    }
}
