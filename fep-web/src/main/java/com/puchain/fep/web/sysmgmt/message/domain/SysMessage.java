package com.puchain.fep.web.sysmgmt.message.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 系统消息 Entity，映射 t_sys_message 表。
 *
 * <p>支持广播（ALL）、指定用户（USER）、指定角色（ROLE）三种投递方式。
 * 逻辑删除通过 messageStatus=DELETED 实现，不物理删除。
 * 参见 PRD v1.3 §5.10.4 消息管理 / §6.4。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Entity
@Table(name = "t_sys_message")
public class SysMessage {

    @Id
    @Column(name = "message_id", length = 32)
    private String messageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 20)
    private MessageType messageType;

    @Column(name = "message_title", nullable = false, length = 200)
    private String messageTitle;

    @Column(name = "message_content", nullable = false, columnDefinition = "TEXT")
    private String messageContent;

    @Column(name = "sender_id", nullable = false, length = 32)
    private String senderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "receiver_type", nullable = false, length = 20)
    private ReceiverType receiverType;

    @Column(name = "receiver_id", length = 32)
    private String receiverId;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_status", nullable = false, length = 20)
    private MessageStatus messageStatus;

    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    /**
     * 无参构造方法（JPA 要求）。
     */
    public SysMessage() {
        /* for JPA */
    }

    // ===== Getters =====

    /**
     * 获取消息唯一标识。
     *
     * @return 消息 ID (UUID 32位)
     */
    public String getMessageId() {
        return messageId;
    }

    /**
     * 获取消息类型。
     *
     * @return 消息类型枚举
     */
    public MessageType getMessageType() {
        return messageType;
    }

    /**
     * 获取消息标题。
     *
     * @return 消息标题
     */
    public String getMessageTitle() {
        return messageTitle;
    }

    /**
     * 获取消息正文。
     *
     * @return 消息正文
     */
    public String getMessageContent() {
        return messageContent;
    }

    /**
     * 获取发送者 ID。
     *
     * @return 发送者用户 ID
     */
    public String getSenderId() {
        return senderId;
    }

    /**
     * 获取接收者类型。
     *
     * @return 接收者类型枚举
     */
    public ReceiverType getReceiverType() {
        return receiverType;
    }

    /**
     * 获取接收者 ID（用户 ID 或角色 ID，ALL 类型时为 null）。
     *
     * @return 接收者 ID，可能为 null
     */
    public String getReceiverId() {
        return receiverId;
    }

    /**
     * 获取消息状态。
     *
     * @return 消息状态枚举
     */
    public MessageStatus getMessageStatus() {
        return messageStatus;
    }

    /**
     * 获取创建时间。
     *
     * @return 创建时间
     */
    public LocalDateTime getCreateTime() {
        return createTime;
    }

    // ===== Setters =====

    /**
     * 设置消息唯一标识。
     *
     * @param messageId 消息 ID (UUID 32位)
     */
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    /**
     * 设置消息类型。
     *
     * @param messageType 消息类型枚举
     */
    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    /**
     * 设置消息标题。
     *
     * @param messageTitle 消息标题
     */
    public void setMessageTitle(String messageTitle) {
        this.messageTitle = messageTitle;
    }

    /**
     * 设置消息正文。
     *
     * @param messageContent 消息正文
     */
    public void setMessageContent(String messageContent) {
        this.messageContent = messageContent;
    }

    /**
     * 设置发送者 ID。
     *
     * @param senderId 发送者用户 ID
     */
    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    /**
     * 设置接收者类型。
     *
     * @param receiverType 接收者类型枚举
     */
    public void setReceiverType(ReceiverType receiverType) {
        this.receiverType = receiverType;
    }

    /**
     * 设置接收者 ID。
     *
     * @param receiverId 接收者 ID（用户 ID 或角色 ID），ALL 类型时传 null
     */
    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    /**
     * 设置消息状态。
     *
     * @param messageStatus 消息状态枚举
     */
    public void setMessageStatus(MessageStatus messageStatus) {
        this.messageStatus = messageStatus;
    }

    /**
     * 设置创建时间。
     *
     * @param createTime 创建时间
     */
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
