package com.puchain.fep.transport.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.puchain.fep.transport.model.TlqChannel;
import com.puchain.fep.transport.model.TlqMessage;
import com.puchain.fep.transport.model.TlqMessageAttributes;

/**
 * Transport API contract tests.
 *
 * <p>Verifies {@link SendResult} record semantics and that all interfaces
 * can be implemented (including lambda-based functional interfaces).</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class TransportApiTest {

    @Test
    @DisplayName("SendResult.ok should be success with null error")
    void sendResult_ok_shouldBeSuccess() {
        final SendResult result = SendResult.ok("MSG-001");

        assertThat(result.success()).isTrue();
        assertThat(result.msgId()).isEqualTo("MSG-001");
        assertThat(result.error()).isNull();
    }

    @Test
    @DisplayName("SendResult.fail should contain error message")
    void sendResult_fail_shouldContainError() {
        final SendResult result = SendResult.fail("MSG-002", "Connection refused");

        assertThat(result.success()).isFalse();
        assertThat(result.msgId()).isEqualTo("MSG-002");
        assertThat(result.error()).isEqualTo("Connection refused");
    }

    @Test
    @DisplayName("TlqProducer should be implementable and return SendResult")
    void interfaces_shouldCompileAndBeImplementable() {
        final TlqProducer producer = message -> SendResult.ok(message.getMsgId());

        final TlqMessageAttributes attrs = TlqMessageAttributes.forRealtime("MSG-003");
        final TlqMessage message = new TlqMessage("<xml/>", attrs, TlqChannel.REALTIME_SEND);

        final SendResult result = producer.send(message);

        assertThat(result.success()).isTrue();
        assertThat(result.msgId()).isEqualTo("MSG-003");
    }

    @Test
    @DisplayName("MessageListener should be a functional interface usable as lambda")
    void messageListener_shouldBeFunctionalInterface() {
        final AtomicReference<String> received = new AtomicReference<>();

        final MessageListener listener = message -> received.set(message.getMsgId());

        final TlqMessageAttributes attrs = TlqMessageAttributes.forRealtime("MSG-004");
        final TlqMessage message = new TlqMessage("<xml/>", attrs, TlqChannel.REALTIME_RECEIVE);

        listener.onMessage(message);

        assertThat(received.get()).isEqualTo("MSG-004");
    }
}
