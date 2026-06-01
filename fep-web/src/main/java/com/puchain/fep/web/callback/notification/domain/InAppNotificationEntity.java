package com.puchain.fep.web.callback.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * 站内信通知实体（通用，{@code category} 区分来源）。
 *
 * <p>首个使用方 {@code CALLBACK_DLQ}（回调死信告警，T12 {@code InAppNotificationListener}
 * 订阅 {@code CallbackDeadLetterEvent} 写入）；表设计为通用站内信，未来 EMAIL/SMS 渠道
 * 扩展目标。每条通知归属单个 {@code userId}（admin 角色用户），{@code read}/{@code readAt}
 * 经 {@link #markRead()} 状态机变更。</p>
 *
 * <p>参见 PRD v1.3 §5.10.7.2d 告警（FR-INFRA-CALLBACK-IN-APP-ALERT）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Entity
@Table(name = "in_app_notification")
public class InAppNotificationEntity {

    /** 通知唯一标识（UUID 32 位无连字符）。 */
    @Id
    @Column(name = "notification_id", length = 32)
    private String notificationId;

    /** 接收用户 id。 */
    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    /** 通知类别（来源识别，如 {@code CALLBACK_DLQ}）。 */
    @Column(name = "category", nullable = false, length = 50)
    private String category;

    /** 级别（{@code ERROR} / {@code WARN} / {@code INFO}）。 */
    @Column(name = "level", nullable = false, length = 20)
    private String level;

    /** 标题。 */
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    /** 正文。 */
    @Column(name = "message", nullable = false, length = 1000)
    private String message;

    /** 关联业务对象 id（如死信行 queue_id），可 null。 */
    @Column(name = "ref_id", length = 64)
    private String refId;

    /** 关联业务对象类型（如 {@code CALLBACK_DLQ_ENTRY}），可 null。 */
    @Column(name = "ref_type", length = 50)
    private String refType;

    /** 已读标记。 */
    @Column(name = "is_read", nullable = false)
    private boolean read;

    /** 创建时间。 */
    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    /** 已读时间（未读为 null）。 */
    @Column(name = "read_at")
    private LocalDateTime readAt;

    /**
     * JPA 要求的无参构造方法。
     */
    protected InAppNotificationEntity() {
        /* for JPA */
    }

    /**
     * 构造一条未读站内信。
     *
     * @param userId   接收用户 id（非 null）
     * @param category 类别（非 null，如 {@code CALLBACK_DLQ}）
     * @param level    级别（非 null，如 {@code ERROR}）
     * @param title    标题（非 null）
     * @param message  正文（非 null）
     * @param refId    关联业务对象 id（可 null）
     * @param refType  关联业务对象类型（可 null）
     * @return 新建未读通知实体
     */
    public static InAppNotificationEntity of(final String userId, final String category,
            final String level, final String title, final String message,
            final String refId, final String refType) {
        final InAppNotificationEntity e = new InAppNotificationEntity();
        e.notificationId = UUID.randomUUID().toString().replace("-", "");
        e.userId = Objects.requireNonNull(userId, "userId");
        e.category = Objects.requireNonNull(category, "category");
        e.level = Objects.requireNonNull(level, "level");
        e.title = Objects.requireNonNull(title, "title");
        e.message = Objects.requireNonNull(message, "message");
        e.refId = refId;
        e.refType = refType;
        e.read = false;
        e.createTime = LocalDateTime.now();
        return e;
    }

    /**
     * 标记已读，记录 {@code readAt}（幂等：重复调用刷新 readAt）。
     */
    public void markRead() {
        this.read = true;
        this.readAt = LocalDateTime.now();
    }

    /**
     * @return 通知唯一标识
     */
    public String getNotificationId() {
        return notificationId;
    }

    /**
     * @return 接收用户 id
     */
    public String getUserId() {
        return userId;
    }

    /**
     * @return 通知类别
     */
    public String getCategory() {
        return category;
    }

    /**
     * @return 级别
     */
    public String getLevel() {
        return level;
    }

    /**
     * @return 标题
     */
    public String getTitle() {
        return title;
    }

    /**
     * @return 正文
     */
    public String getMessage() {
        return message;
    }

    /**
     * @return 关联业务对象 id（可 null）
     */
    public String getRefId() {
        return refId;
    }

    /**
     * @return 关联业务对象类型（可 null）
     */
    public String getRefType() {
        return refType;
    }

    /**
     * @return 是否已读
     */
    public boolean isRead() {
        return read;
    }

    /**
     * @return 创建时间
     */
    public LocalDateTime getCreateTime() {
        return createTime;
    }

    /**
     * @return 已读时间（未读为 null）
     */
    public LocalDateTime getReadAt() {
        return readAt;
    }
}
