package com.puchain.fep.transport.api;

import com.puchain.fep.transport.model.TlqChannel;
import com.puchain.fep.transport.model.TlqMessage;
import com.puchain.fep.transport.model.TlqMessageAttributes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RetryableProducer}.
 *
 * @author FEP Team
 * @since 1.0.0
 */
class RetryableProducerTest {

    @Test
    @DisplayName("send: success on first attempt should not retry and no dead letters")
    void send_success_shouldNotRetry() {
        final AtomicInteger callCount = new AtomicInteger(0);
        final List<TlqMessage> deadLetters = new ArrayList<>();

        final TlqProducer inner = message -> {
            callCount.incrementAndGet();
            return SendResult.ok(message.getMsgId());
        };
        final DeadLetterHandler dlh = (message, reason) -> deadLetters.add(message);
        final RetryableProducer producer = new RetryableProducer(inner, dlh, 3, 10L);

        final SendResult result = producer.send(sampleMessage());

        assertThat(result.success()).isTrue();
        assertThat(callCount.get()).isEqualTo(1);
        assertThat(deadLetters).isEmpty();
    }

    @Test
    @DisplayName("send: fail first 2 then succeed should retry and no dead letters")
    void send_failThenSucceed_shouldRetry() {
        final AtomicInteger callCount = new AtomicInteger(0);
        final List<TlqMessage> deadLetters = new ArrayList<>();

        final TlqProducer inner = message -> {
            final int count = callCount.incrementAndGet();
            if (count <= 2) {
                return SendResult.fail(message.getMsgId(), "transient error #" + count);
            }
            return SendResult.ok(message.getMsgId());
        };
        final DeadLetterHandler dlh = (message, reason) -> deadLetters.add(message);
        final RetryableProducer producer = new RetryableProducer(inner, dlh, 3, 10L);

        final SendResult result = producer.send(sampleMessage());

        assertThat(result.success()).isTrue();
        assertThat(callCount.get()).isEqualTo(3);
        assertThat(deadLetters).isEmpty();
    }

    @Test
    @DisplayName("send: always fail should exhaust retries and route to dead letter")
    void send_alwaysFail_shouldGoToDeadLetter() {
        final AtomicInteger callCount = new AtomicInteger(0);
        final List<TlqMessage> deadLetters = new ArrayList<>();

        final TlqProducer inner = message -> {
            callCount.incrementAndGet();
            return SendResult.fail(message.getMsgId(), "persistent error");
        };
        final DeadLetterHandler dlh = (message, reason) -> deadLetters.add(message);
        final RetryableProducer producer = new RetryableProducer(inner, dlh, 3, 10L);

        final SendResult result = producer.send(sampleMessage());

        assertThat(result.success()).isFalse();
        assertThat(callCount.get()).isEqualTo(4); // 1 initial + 3 retries
        assertThat(deadLetters).hasSize(1);
    }

    /**
     * Create a sample TLQ message for testing.
     *
     * @return a test message
     */
    private TlqMessage sampleMessage() {
        final TlqMessageAttributes attrs = TlqMessageAttributes.forRealtime("MSG001");
        return new TlqMessage("<CFX/>", attrs, TlqChannel.REALTIME_SEND);
    }
}
