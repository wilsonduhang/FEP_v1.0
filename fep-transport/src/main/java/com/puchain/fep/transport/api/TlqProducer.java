package com.puchain.fep.transport.api;

import com.puchain.fep.transport.model.TlqMessage;

/**
 * TLQ 消息生产者接口。
 *
 * <p>负责将 {@link TlqMessage} 发送到 TLQ 队列，
 * 返回 {@link SendResult} 表示发送结果。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public interface TlqProducer {

    /**
     * 发送消息到 TLQ 队列。
     *
     * @param message 待发送的消息，不能为 {@code null}
     * @return 发送结果
     */
    SendResult send(TlqMessage message);
}
