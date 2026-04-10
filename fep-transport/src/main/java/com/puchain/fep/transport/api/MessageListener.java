package com.puchain.fep.transport.api;

import com.puchain.fep.transport.model.TlqMessage;

/**
 * TLQ 消息监听器（推模式回调）。
 *
 * <p>函数式接口，用于 {@link TlqConsumer#subscribe} 的推模式消费。
 * 当收到消息时，TLQ Consumer 会调用 {@link #onMessage(TlqMessage)}。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@FunctionalInterface
public interface MessageListener {

    /**
     * 处理收到的消息。
     *
     * @param message 收到的 TLQ 消息，不能为 {@code null}
     */
    void onMessage(TlqMessage message);
}
