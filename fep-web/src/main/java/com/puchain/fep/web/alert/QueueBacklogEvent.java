package com.puchain.fep.web.alert;

import java.time.LocalDateTime;

/**
 * 队列积压告警事件：当某队列积压深度首次<b>上穿</b>阈值时由 {@code QueueBacklogMonitor} 发布
 * （边沿触发，持续高位不重发，回落后 re-arm），由 {@code QueueBacklogAlertEvaluator} 订阅
 * 按 {@code t_sys_alert_rule} 配置分发 IN_APP / EMAIL / SMS 告警。
 *
 * <p>事件解耦：监控器不直接依赖告警分发，仅 {@code publishEvent}（镜像 {@code CallbackDeadLetterEvent}
 * / {@code TlqOutboundDeadLetterEvent} 设计）。参见 PRD v1.3 §5.9.1 告警管理（FR-WEB-TLQ-FAULT）。</p>
 *
 * @param queue        积压队列
 * @param backlogDepth 触发时积压条数
 * @param threshold    生效阈值（来自 yaml {@code fep.alert.queue-backlog.threshold}）
 * @param occurredAt   采样时间
 * @author FEP Team
 * @since 1.0.0
 */
public record QueueBacklogEvent(QueueBacklogQueue queue, long backlogDepth, long threshold,
        LocalDateTime occurredAt) {
}
