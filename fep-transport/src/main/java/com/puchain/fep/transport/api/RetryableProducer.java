package com.puchain.fep.transport.api;

import com.puchain.fep.transport.model.TlqMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * TLQ 消息生产者装饰器，提供指数退避重试和死信路由能力。
 *
 * <p>包装一个 {@link TlqProducer} 委托对象，当发送失败时按指数退避策略重试，
 * 重试耗尽后将消息路由到 {@link DeadLetterHandler}。</p>
 *
 * <p>退避公式：{@code delay = baseDelayMs × 2^(attempt-1)}，
 * 其中 attempt 从 1 开始计数。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class RetryableProducer implements TlqProducer {

    private static final Logger log = LoggerFactory.getLogger(RetryableProducer.class);

    private final TlqProducer delegate;
    private final DeadLetterHandler deadLetterHandler;
    private final int maxRetries;
    private final long baseDelayMs;

    /**
     * 构造可重试的生产者。
     *
     * @param delegate         内部委托生产者，不能为 {@code null}
     * @param deadLetterHandler 死信处理器，不能为 {@code null}
     * @param maxRetries       最大重试次数，必须 &gt;= 0
     * @param baseDelayMs      基础延迟毫秒数，必须 &gt; 0
     * @throws NullPointerException     如果 delegate 或 deadLetterHandler 为 null
     * @throws IllegalArgumentException 如果 maxRetries &lt; 0 或 baseDelayMs &lt;= 0
     */
    public RetryableProducer(final TlqProducer delegate,
                             final DeadLetterHandler deadLetterHandler,
                             final int maxRetries,
                             final long baseDelayMs) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        this.deadLetterHandler = Objects.requireNonNull(deadLetterHandler,
                "deadLetterHandler must not be null");
        if (maxRetries < 0) {
            throw new IllegalArgumentException("maxRetries must be >= 0, got " + maxRetries);
        }
        if (baseDelayMs <= 0) {
            throw new IllegalArgumentException("baseDelayMs must be > 0, got " + baseDelayMs);
        }
        this.maxRetries = maxRetries;
        this.baseDelayMs = baseDelayMs;
    }

    /**
     * 发送消息到 TLQ 队列，失败时按指数退避重试。
     *
     * <p>流程：</p>
     * <ol>
     *   <li>调用委托生产者发送，成功则立即返回</li>
     *   <li>失败时重试最多 {@code maxRetries} 次，延迟按指数退避递增</li>
     *   <li>重试耗尽后将消息交给死信处理器，并返回失败结果</li>
     * </ol>
     *
     * @param message 待发送的消息，不能为 {@code null}
     * @return 发送结果
     */
    @Override
    public SendResult send(final TlqMessage message) {
        final String msgId = message.getMsgId();
        SendResult result = delegate.send(message);
        if (result.success()) {
            return result;
        }

        String lastError = result.error();
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            final long delay = baseDelayMs * (1L << (attempt - 1));
            log.warn("Send failed for msgId={}, retry {}/{} after {}ms: {}",
                    msgId, attempt, maxRetries, delay, lastError);

            sleep(delay);

            result = delegate.send(message);
            if (result.success()) {
                return result;
            }
            lastError = result.error();
        }

        log.error("Send exhausted all {} retries for msgId={}, routing to dead letter: {}",
                maxRetries, msgId, lastError);
        deadLetterHandler.handle(message, lastError);
        return SendResult.fail(msgId, lastError);
    }

    /**
     * 执行退避等待，正确处理中断。
     *
     * @param delayMs 等待毫秒数
     */
    private void sleep(final long delayMs) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Retry sleep interrupted for RetryableProducer");
        }
    }
}
