package com.puchain.fep.collector.adapter.mq;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * MQ 消息载体（不可变 record） — 由 {@link MqCollectorAdapter} 子类
 * 在 {@code pollMessages} 阶段从 MQ broker 拉取并构造。
 *
 * <p><b>契约：子类负责将 byte[] / JSON 解析为 {@code Map<String, Object>} payload。</b>
 * 本 record 不假设 wire 格式（MQ 多数采用 byte[] / 文本 / JSON），将解析责任下推到子类
 * 是有意为之 — RabbitMQ / Kafka 子类各自决定是用 Jackson / 自研 schema / Protobuf 等。
 *
 * <p><b>不可变性保证：</b>
 * <ul>
 *   <li>compact 构造函数 {@link Objects#requireNonNull} 校验所有字段</li>
 *   <li>{@link #payload} 在构造时执行 {@link Map#copyOf} 防御拷贝（与
 *       {@link com.puchain.fep.collector.support.CollectionRecord} 一致）</li>
 * </ul>
 *
 * @param messageId  MQ broker 侧消息唯一标识（如 RabbitMQ delivery tag 转字符串、
 *                   Kafka {@code topic-partition-offset}）— 用于 ack / 去重；非 null
 * @param payload    业务字段 Map（已由子类从 byte[] / JSON 解析得到）；非 null，可为空 Map
 * @param enqueuedAt MQ 侧消息入队时刻（如 RabbitMQ {@code timestamp} header / Kafka
 *                   {@code record.timestamp()}）；非 null，用于审计 / 端到端时延统计
 *
 * @author FEP Team
 * @since 1.0.0
 */
public record MqMessage(String messageId, Map<String, Object> payload, Instant enqueuedAt) {

    /**
     * compact 构造函数 — null 校验 + payload 防御拷贝。
     */
    public MqMessage {
        Objects.requireNonNull(messageId, "messageId");
        Objects.requireNonNull(payload, "payload");
        Objects.requireNonNull(enqueuedAt, "enqueuedAt");
        // 防御拷贝：外部修改原始 Map 不会影响 record 内部状态
        payload = Map.copyOf(payload);
    }
}
