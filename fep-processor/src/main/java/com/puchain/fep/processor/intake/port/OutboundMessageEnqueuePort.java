package com.puchain.fep.processor.intake.port;

/**
 * 出站报文入队 Port（intake.port 跨模块契约，hexagonal driver port）。
 *
 * <p>P4 fep-collector 调用方：组装完成后通过此 Port 把
 * {@link OutboundMessageEnvelope} 交付给 fep-web 持久化适配器
 * （{@code JpaOutboundMessageEnqueueService}，由 T7a 实现）。
 *
 * <p><b>幂等语义：</b>实现必须基于 {@code envelope.idempotencyKey} 做唯一约束 —
 * 第二次提交同 key 应返回 {@link EnqueueResult.Status#DUPLICATE}（或抛
 * {@code FepBusinessException(COLLECT_DUPLICATE_KEY)} 由调用方按 at-least-once 兜底）。
 *
 * <p><b>事务语义：</b>实现侧建议 {@code @Transactional(propagation=REQUIRES_NEW)}
 * 以独立事务持久化（详见 T7a Plan §5）。
 *
 * @author FEP Team
 * @since 1.0.0
 */
public interface OutboundMessageEnqueuePort {

    /**
     * 提交出站报文 envelope 入队。
     *
     * @param envelope 出站报文载体（非 null）
     * @return 入队结果（含 queueId + status）
     * @throws com.puchain.fep.common.exception.FepBusinessException 当幂等冲突或持久化失败时
     */
    EnqueueResult submit(OutboundMessageEnvelope envelope);
}
