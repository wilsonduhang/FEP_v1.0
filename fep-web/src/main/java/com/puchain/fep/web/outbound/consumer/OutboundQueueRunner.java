package com.puchain.fep.web.outbound.consumer;

/**
 * P5 outbound queue 单条目执行契约 (P5 T2).
 *
 * <p>{@link OutboundQueueConsumer#poll()} 在每轮 poll 取得一批 {@code queue_id} 后，
 * 逐条调用 {@link #run(String)} 完成"读 entity → 组 CFX 报文 → 签名 → TLQ 发送
 * → 回写状态"的全流程。本接口刻意只暴露 {@code queueId} 入参，让生产实现承担状态机
 * 与异常分流职责。</p>
 *
 * <p>实现类生命周期：</p>
 * <ul>
 *   <li>T2 阶段：{@code OutboundQueueRunnerStubConfiguration} 占位（已在 T9 删除）</li>
 *   <li>T9 阶段：生产实现 {@link OutboundQueueRunnerImpl} 通过 {@code @Component} 注册</li>
 * </ul>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public interface OutboundQueueRunner {

    /**
     * 执行单条 outbound queue 条目的发送流水。
     *
     * <p>实现方应在内部完成幂等保护 / 状态变更 / 异常→DLQ 分流；本方法对外只通过
     * 抛出 {@link RuntimeException} 表达失败，由 {@link OutboundQueueConsumer#poll()}
     * 的 try/catch 兜底以保证单行异常不影响其它行。</p>
     *
     * @param queueId 已被 {@link OutboundQueueRepository#claimBatch(int)} 持锁的 queue_id
     */
    void run(String queueId);
}
