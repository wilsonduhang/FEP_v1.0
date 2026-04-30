package com.puchain.fep.collector.support;

/**
 * 采集层运行指标 point-in-time 快照（不可变 record）。
 *
 * <p>由 {@link CollectionMetrics#snapshot()} 构造，反映调用瞬间各 LongAdder 的累计值。
 * 后续 {@code incXxx} 调用不会影响已创建的 snapshot 实例。
 *
 * <p><b>字段语义：</b>
 * <ul>
 *   <li>{@code collected} — 已从源系统采集的原始记录条数</li>
 *   <li>{@code assembled} — 已成功组装为 FEP 报文的记录条数</li>
 *   <li>{@code submitted} — 已成功提交到下游（TLQ / DB / 队列）的报文条数</li>
 *   <li>{@code failed}    — 处理过程中失败的记录条数（采集 / 组装 / 提交任一环节）</li>
 *   <li>{@code skipped}   — 被业务规则主动跳过的记录条数（如黑名单、不满足触发条件）</li>
 * </ul>
 *
 * <p>record 自身的不可变性（final 字段 + 隐式 deep copy 不需要）保证 snapshot
 * 在跨线程传递时无可见性问题。
 *
 * @param collected 已采集记录条数
 * @param assembled 已组装报文条数
 * @param submitted 已提交报文条数
 * @param failed    失败记录条数
 * @param skipped   跳过记录条数
 * @author FEP Team
 * @since 1.0.0
 */
public record CollectionMetricsSnapshot(
        long collected,
        long assembled,
        long submitted,
        long failed,
        long skipped
) {
}
