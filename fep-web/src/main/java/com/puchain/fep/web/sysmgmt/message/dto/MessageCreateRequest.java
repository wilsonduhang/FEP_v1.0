package com.puchain.fep.web.sysmgmt.message.dto;

import com.puchain.fep.web.sysmgmt.message.domain.MessageType;
import com.puchain.fep.web.sysmgmt.message.domain.ReceiverType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 消息发布请求 DTO。
 *
 * <p>receiverType=ALL 时 receiverId 可为 null；USER/ROLE 类型必须提供 receiverId。
 * 参见 PRD v1.3 §5.10.4 消息管理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class MessageCreateRequest {

    @NotNull(message = "消息类型不能为空")
    private MessageType messageType;

    @NotBlank(message = "消息标题不能为空")
    @Size(max = 200, message = "消息标题最长 200 字符")
    private String title;

    @NotBlank(message = "消息内容不能为空")
    private String content;

    @NotNull(message = "接收者类型不能为空")
    private ReceiverType receiverType;

    /** 接收者 ID（USER 时为用户 ID，ROLE 时为角色 ID，ALL 时为 null）。 */
    private String receiverId;

    /**
     * 获取消息类型。
     *
     * @return 消息类型枚举
     */
    public MessageType getMessageType() {
        return messageType;
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
     * 获取消息标题。
     *
     * @return 消息标题
     */
    public String getTitle() {
        return title;
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
     * 获取消息内容。
     *
     * @return 消息内容
     */
    public String getContent() {
        return content;
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
     * 获取接收者类型。
     *
     * @return 接收者类型枚举
     */
    public ReceiverType getReceiverType() {
        return receiverType;
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
     * 获取接收者 ID。
     *
     * @return 接收者 ID，ALL 类型时为 null
     */
    public String getReceiverId() {
        return receiverId;
    }

    /**
     * 设置接收者 ID。
     *
     * @param receiverId 接收者 ID
     */
    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }
}
