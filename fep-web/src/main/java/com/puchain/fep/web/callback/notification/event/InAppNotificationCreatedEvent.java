package com.puchain.fep.web.callback.notification.event;

import java.time.Instant;
import java.util.Objects;

/**
 * 站内通知创建事件 — 一条 {@code in_app_notification} 落库后发布，承载实时推送所需的
 * 最小信息（每条通知一个事件，对应单个收件 userId）。
 *
 * <p>由 {@code CallbackInAppAlertChannel} 在 {@code notifRepo.save} 后发布，
 * 经 {@code DashboardNotificationPushListener} 的
 * {@code @TransactionalEventListener(AFTER_COMMIT)} 消费 → WebSocket 推送到目标用户
 * 活跃会话（B-8，FR-WEB-DASH-REFRESH，PRD §5.2.5 实时告警层）。</p>
 *
 * @param userId         收件用户 id（非 null）
 * @param notificationId 通知唯一标识（非 null）
 * @param occurredAt     事件发生时刻（非 null）
 * @author FEP Team
 * @since 1.0.0
 */
public record InAppNotificationCreatedEvent(String userId, String notificationId, Instant occurredAt) {

    /**
     * 紧凑构造器：非空校验。
     */
    public InAppNotificationCreatedEvent {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(notificationId, "notificationId");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
