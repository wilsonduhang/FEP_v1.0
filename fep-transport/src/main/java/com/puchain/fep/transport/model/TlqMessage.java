package com.puchain.fep.transport.model;

import java.util.Objects;

/**
 * TLQ 消息封装，不可变对象。
 *
 * <p>将消息载荷（xmlstr/xmlstr1/xmlstr2 拼接后的完整 XML 字符串）、
 * 消息属性和通道信息组合为一个完整的消息对象。</p>
 *
 * <p>所有字段通过构造函数注入并做非空校验，构造后不可修改。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class TlqMessage {

    private final String payload;
    private final TlqMessageAttributes attributes;
    private final TlqChannel channel;

    /**
     * 构造 TLQ 消息。
     *
     * @param payload    消息载荷（XML 字符串），不能为 {@code null}
     * @param attributes 消息属性，不能为 {@code null}
     * @param channel    通信通道，不能为 {@code null}
     * @throws NullPointerException 如果任意参数为 null
     */
    public TlqMessage(final String payload, final TlqMessageAttributes attributes, final TlqChannel channel) {
        this.payload = Objects.requireNonNull(payload, "payload must not be null");
        this.attributes = Objects.requireNonNull(attributes, "attributes must not be null");
        this.channel = Objects.requireNonNull(channel, "channel must not be null");
    }

    /**
     * 获取消息载荷。
     *
     * @return XML 字符串载荷
     */
    public String getPayload() {
        return payload;
    }

    /**
     * 获取消息属性。
     *
     * @return 消息属性
     */
    public TlqMessageAttributes getAttributes() {
        return attributes;
    }

    /**
     * 获取通信通道。
     *
     * @return 通道枚举
     */
    public TlqChannel getChannel() {
        return channel;
    }

    /**
     * 快捷获取消息 ID。
     *
     * @return 消息 ID，等价于 {@code getAttributes().getMsgId()}
     */
    public String getMsgId() {
        return attributes.getMsgId();
    }

    @Override
    public String toString() {
        return "TlqMessage{msgId=" + attributes.getMsgId()
                + ", channel=" + channel
                + ", payloadLength=" + payload.length()
                + ", zip=" + attributes.isZip()
                + ", encrypt=" + attributes.isEncrypt()
                + '}';
    }
}
