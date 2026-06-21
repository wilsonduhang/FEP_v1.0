package com.puchain.fep.web.outbound.event;

import java.time.LocalDateTime;

/**
 * TLQ 出站报文死信事件：当一条 {@code outbound_message_queue} 行因发送重试耗尽进入
 * {@code DEAD_LETTER} 终态时，由 {@code OutboundQueueRunnerImpl} 发布（Spring
 * {@code ApplicationEventPublisher}），由 {@code TlqOutboundAlertEvaluator} 订阅，
 * 按 {@code t_sys_alert_rule} 配置分发 IN_APP / EMAIL / SMS 告警。
 *
 * <p>事件解耦：Runner 不直接依赖告警模块，仅 {@code publishEvent}；订阅方按需扩展不改 Runner
 * （镜像 callback 侧 {@code CallbackDeadLetterEvent} 设计）。参见 PRD v1.3 §5.7 TLQ 节点管理
 * › 故障处理（自动告警）/ §5.9.1 告警管理（FR-WEB-TLQ-FAULT）。</p>
 *
 * @param queueId    死信行 queue_id（{@code outbound_message_queue} PK，VARCHAR(32) UUID-no-dash）
 * @param msgNo      报文号（{@code OutboundMessageQueueEntity} 当前无该字段，固定 {@code null}；
 *                   后续如在 entity 增字段可回填真值）
 * @param retryCount 进入死信时累计重试次数
 * @param lastError  最后错误摘要（取 {@code entity.errorMessage}，已截断，可 {@code null}）
 * @param occurredAt 死信发生时间
 * @author FEP Team
 * @since 1.0.0
 */
public record TlqOutboundDeadLetterEvent(
        String queueId,
        String msgNo,
        int retryCount,
        String lastError,
        LocalDateTime occurredAt) {
}
