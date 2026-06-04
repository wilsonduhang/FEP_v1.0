package com.puchain.fep.web.callback.notification.dto;

import com.puchain.fep.web.callback.notification.domain.CallbackNotificationEntity;

import java.time.LocalDateTime;

/**
 * 站内通知响应 DTO（当前登录用户视角）。
 *
 * <p>投影 {@link CallbackNotificationEntity} 的展示字段，不含 {@code userId}（恒为调用方自身，
 * 由认证上下文确定）。参见 PRD v1.3 §5.5.3 回调可靠性告警（FR-INFRA-CALLBACK-ALERT）。</p>
 *
 * @param notificationId 通知唯一标识
 * @param category       通知类别（如 {@code CALLBACK_DLQ}）
 * @param level          级别（如 {@code ERROR}）
 * @param title          标题
 * @param message        正文摘要
 * @param refId          关联业务对象 id（如死信行 queueId），可空
 * @param refType        关联业务对象类型（如 {@code CALLBACK_DLQ_ENTRY}），可空
 * @param read           是否已读
 * @param createTime     创建时间
 * @param readAt         已读时间（未读为 null）
 * @author FEP Team
 * @since 1.0.0
 */
public record CallbackNotificationResponse(String notificationId, String category, String level,
        String title, String message, String refId, String refType, boolean read,
        LocalDateTime createTime, LocalDateTime readAt) {

    /**
     * 由通知实体投影为响应 DTO。
     *
     * @param e 通知实体，非空
     * @return 响应 DTO
     */
    public static CallbackNotificationResponse from(final CallbackNotificationEntity e) {
        return new CallbackNotificationResponse(e.getNotificationId(), e.getCategory(), e.getLevel(),
                e.getTitle(), e.getMessage(), e.getRefId(), e.getRefType(), e.isRead(),
                e.getCreateTime(), e.getReadAt());
    }
}
