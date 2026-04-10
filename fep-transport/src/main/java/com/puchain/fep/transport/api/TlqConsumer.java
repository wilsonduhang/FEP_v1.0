package com.puchain.fep.transport.api;

import java.time.Duration;
import java.util.Optional;

import com.puchain.fep.transport.model.TlqChannel;
import com.puchain.fep.transport.model.TlqMessage;

/**
 * TLQ 消息消费者接口，支持拉模式和推模式。
 *
 * <p>拉模式：调用 {@link #receive(TlqChannel, Duration)} 主动拉取单条消息。</p>
 * <p>推模式：调用 {@link #subscribe(TlqChannel, MessageListener)} 注册监听器，
 * 由 Consumer 自动分发消息到 {@link MessageListener}。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public interface TlqConsumer {

    /**
     * 拉模式：从指定通道拉取一条消息。
     *
     * @param channel 通信通道，不能为 {@code null}
     * @param timeout 最大等待时间，不能为 {@code null}
     * @return 消息（如果在超时时间内收到），否则 {@link Optional#empty()}
     */
    Optional<TlqMessage> receive(TlqChannel channel, Duration timeout);

    /**
     * 推模式：订阅指定通道的消息。
     *
     * @param channel  通信通道，不能为 {@code null}
     * @param listener 消息监听器，不能为 {@code null}
     */
    void subscribe(TlqChannel channel, MessageListener listener);

    /**
     * 取消指定通道的订阅。
     *
     * @param channel 通信通道，不能为 {@code null}
     */
    void unsubscribe(TlqChannel channel);
}
