package com.puchain.fep.transport.api;

import com.puchain.fep.transport.model.TlqMessage;

/**
 * 死信处理器接口。
 *
 * <p>当消息处理失败且超过重试次数时，由死信处理器接管。
 * 实现者应记录失败原因并将消息持久化到死信队列或数据库。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public interface DeadLetterHandler {

    /**
     * 处理死信消息。
     *
     * @param message 失败的消息，不能为 {@code null}
     * @param reason  失败原因，不能为 {@code null}
     */
    void handle(TlqMessage message, String reason);
}
