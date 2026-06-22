package com.puchain.fep.web.tlq.node.event;

import java.time.LocalDateTime;

/**
 * TLQ 节点离线事件：{@code TlqNodeService.changeStatus} 将节点置为 OFFLINE 后发布，
 * 由 {@code TlqNodeOfflineAlertEvaluator} 订阅并按 {@code t_sys_alert_rule} 配置告警。
 *
 * <p>镜像 {@code TlqOutboundDeadLetterEvent}。承载不可变快照（事件发布后节点可能再次变更，
 * 故快照值在发布时即固化）。参见 PRD v1.3 §5.7 TLQ 节点管理 › 故障处理（FR-WEB-TLQ-FAULT）。</p>
 *
 * @param nodeId        节点 ID（in_app_notification.ref_id）
 * @param nodeName      节点名称（告警标题展示）
 * @param lastHeartbeat 最后心跳时间（可 null，节点从未上报心跳时）
 * @param occurredAt    离线事件发生时间
 * @author FEP Team
 * @since 1.0.0
 */
public record TlqNodeOfflineEvent(
        String nodeId,
        String nodeName,
        LocalDateTime lastHeartbeat,
        LocalDateTime occurredAt) {
}
