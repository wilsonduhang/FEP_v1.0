package com.puchain.fep.collector.adapter.mq;

import com.puchain.fep.collector.support.CollectionMetrics;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存版 MQ 采集适配器 — <b>仅供测试 / 单元演示用</b>。
 *
 * <p>生产环境的 RabbitMQ / Kafka 子类延后到 P5+（Plan §T4 deferred）。本类用于：
 * <ul>
 *   <li>{@link MqCollectorAdapter} 抽象基类的单元测试驱动</li>
 *   <li>装配链 / 调度器 / 端到端流程的本地演示</li>
 * </ul>
 *
 * <p><b>队列实现：</b>{@link ConcurrentLinkedDeque} — Plan §T4 #3 写的是 {@code ArrayDeque}，
 * 但 ArrayDeque 非线程安全；本测试桩可能被多线程调用（调度器 / 测试并发）—
 * 选择 {@link ConcurrentLinkedDeque} 是对 Plan 意图的更安全实现。
 *
 * <p><b>committedIds 暴露：</b>{@link #committedIds()} 返回不可变快照，供测试断言
 * 子类 {@link #commit(List)} 被调用时收到正确的 messageIds。
 *
 * <p><b>非 Spring Bean：</b>本类不带 {@code @Component} 注解，与 T1/T2/T3 先例一致 —
 * 测试中显式 new。
 *
 * @author FEP Team
 * @since 1.0.0
 */
public final class InMemoryMqCollectorAdapter extends MqCollectorAdapter {

    /** 内存队列 — FIFO 顺序，线程安全（{@link ConcurrentLinkedDeque}）。 */
    private final Deque<MqMessage> queue = new ConcurrentLinkedDeque<>();

    /** 已 commit 的 messageId 集合 — 测试用，{@link #commit(List)} 累加。 */
    private final Set<String> committedIds = ConcurrentHashMap.newKeySet();

    /**
     * 构造内存 MQ 适配器。
     *
     * @param adapterId       适配器 ID（非 null / 非空）
     * @param payloadDataType 报文数据类型（非 null）
     * @param metrics         指标聚合器（非 null）
     * @throws NullPointerException 任一参数为 null
     */
    public InMemoryMqCollectorAdapter(
            final String adapterId,
            final String payloadDataType,
            final CollectionMetrics metrics) {
        super(adapterId, payloadDataType, metrics);
    }

    /**
     * 测试投放消息（追加到队尾）。
     *
     * <p><b>仅供测试调用</b> — 生产 MQ 子类不会有此方法，消息由 broker 推 / 拉。
     *
     * @param message 待入队消息（非 null）
     * @throws NullPointerException 当 {@code message} 为 null
     */
    public void enqueueForTest(final MqMessage message) {
        Objects.requireNonNull(message, "message");
        queue.addLast(message);
    }

    /**
     * 已 commit 的 messageId 不可变快照（测试断言用）。
     *
     * @return {@link Set#copyOf} 不可变快照（非 null）
     */
    public Set<String> committedIds() {
        return Set.copyOf(committedIds);
    }

    @Override
    protected List<MqMessage> pollMessages(final int max) {
        if (max <= 0) {
            return List.of();
        }
        final List<MqMessage> drained = new ArrayList<>(Math.min(max, queue.size()));
        for (int i = 0; i < max; i++) {
            final MqMessage msg = queue.pollFirst();
            if (msg == null) {
                break;
            }
            drained.add(msg);
        }
        return List.copyOf(drained);
    }

    @Override
    protected void commit(final List<String> messageIds) {
        Objects.requireNonNull(messageIds, "messageIds");
        committedIds.addAll(messageIds);
    }
}
