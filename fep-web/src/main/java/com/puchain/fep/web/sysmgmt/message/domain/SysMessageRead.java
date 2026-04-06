package com.puchain.fep.web.sysmgmt.message.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 消息已读追踪 Entity，映射 t_sys_message_read 表。
 *
 * <p>每条记录表示某用户已阅读某消息，通过 (message_id, user_id) 唯一约束防止重复。
 * 参见 PRD v1.3 §5.10.4 消息管理 / §6.4。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Entity
@Table(name = "t_sys_message_read")
public class SysMessageRead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "message_id", nullable = false, length = 32)
    private String messageId;

    @Column(name = "user_id", nullable = false, length = 32)
    private String userId;

    @Column(name = "read_time", nullable = false)
    private LocalDateTime readTime;

    /**
     * 无参构造方法（JPA 要求）。
     */
    public SysMessageRead() {
        /* for JPA */
    }

    /**
     * 便捷构造方法，自动设置阅读时间为当前时间。
     *
     * @param messageId 消息 ID
     * @param userId    阅读用户 ID
     */
    public SysMessageRead(final String messageId, final String userId) {
        this.messageId = messageId;
        this.userId = userId;
        this.readTime = LocalDateTime.now();
    }

    // ===== Getters =====

    /**
     * 获取主键。
     *
     * @return 自增主键
     */
    public Long getId() {
        return id;
    }

    /**
     * 获取消息 ID。
     *
     * @return 消息 ID
     */
    public String getMessageId() {
        return messageId;
    }

    /**
     * 获取用户 ID。
     *
     * @return 用户 ID
     */
    public String getUserId() {
        return userId;
    }

    /**
     * 获取阅读时间。
     *
     * @return 阅读时间
     */
    public LocalDateTime getReadTime() {
        return readTime;
    }

    // ===== Setters =====

    /**
     * 设置主键。
     *
     * @param id 自增主键
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 设置消息 ID。
     *
     * @param messageId 消息 ID
     */
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    /**
     * 设置用户 ID。
     *
     * @param userId 用户 ID
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * 设置阅读时间。
     *
     * @param readTime 阅读时间
     */
    public void setReadTime(LocalDateTime readTime) {
        this.readTime = readTime;
    }
}
