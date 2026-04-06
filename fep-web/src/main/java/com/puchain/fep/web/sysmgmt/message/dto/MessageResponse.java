package com.puchain.fep.web.sysmgmt.message.dto;

import com.puchain.fep.web.sysmgmt.message.domain.MessageType;
import com.puchain.fep.web.sysmgmt.message.domain.ReceiverType;
import com.puchain.fep.web.sysmgmt.message.domain.SysMessage;

import java.time.LocalDateTime;

/**
 * 消息响应 DTO。
 *
 * <p>包含消息基本信息以及当前用户的已读状态。
 * 参见 PRD v1.3 §5.10.4 消息管理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class MessageResponse {

    private String messageId;
    private MessageType messageType;
    private String title;
    private String content;
    private String senderId;
    private ReceiverType receiverType;
    private String receiverId;
    private boolean read;
    private LocalDateTime createTime;

    /**
     * 从 SysMessage Entity 构建响应 DTO。
     *
     * @param entity 消息 Entity
     * @param isRead 当前用户是否已读该消息
     * @return 消息响应 DTO
     */
    public static MessageResponse from(final SysMessage entity, final boolean isRead) {
        MessageResponse resp = new MessageResponse();
        resp.setMessageId(entity.getMessageId());
        resp.setMessageType(entity.getMessageType());
        resp.setTitle(entity.getMessageTitle());
        resp.setContent(entity.getMessageContent());
        resp.setSenderId(entity.getSenderId());
        resp.setReceiverType(entity.getReceiverType());
        resp.setReceiverId(entity.getReceiverId());
        resp.setRead(isRead);
        resp.setCreateTime(entity.getCreateTime());
        return resp;
    }

    // ===== Getters =====

    /**
     * 获取消息 ID。
     *
     * @return 消息 ID
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
    public String getTitle() {
        return title;
    }

    /**
     * 获取消息内容。
     *
     * @return 消息内容
     */
    public String getContent() {
        return content;
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
     * 获取接收者 ID。
     *
     * @return 接收者 ID，ALL 类型时为 null
     */
    public String getReceiverId() {
        return receiverId;
    }

    /**
     * 获取当前用户是否已读该消息。
     *
     * @return true 已读，false 未读
     */
    public boolean isRead() {
        return read;
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
     * 设置消息 ID。
     *
     * @param messageId 消息 ID
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
     * @param title 消息标题
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * 设置消息内容。
     *
     * @param content 消息内容
     */
    public void setContent(String content) {
        this.content = content;
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
     * @param receiverId 接收者 ID
     */
    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    /**
     * 设置已读状态。
     *
     * @param read true 已读，false 未读
     */
    public void setRead(boolean read) {
        this.read = read;
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
