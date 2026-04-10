package com.puchain.fep.transport.mock;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.puchain.fep.transport.api.SendResult;
import com.puchain.fep.transport.model.NodeState;
import com.puchain.fep.transport.model.TlqChannel;
import com.puchain.fep.transport.model.TlqMessage;
import com.puchain.fep.transport.model.TlqMessageAttributes;

/**
 * Unit tests for the in-memory Mock TLQ implementation.
 *
 * <p>Plain unit tests — no Spring context required.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class InMemoryTransportTest {

    private InMemoryMessageBroker broker;
    private InMemoryTlqProducer producer;
    private InMemoryTlqConsumer consumer;

    @BeforeEach
    void setUp() {
        broker = new InMemoryMessageBroker();
        producer = new InMemoryTlqProducer(broker);
        consumer = new InMemoryTlqConsumer(broker);
    }

    @Test
    void sendAndReceive_shouldDeliverMessage() {
        final TlqMessage msg = createMessage("MSG-001", "<xml>hello</xml>", TlqChannel.REALTIME_SEND);

        final SendResult result = producer.send(msg);

        assertThat(result.success()).isTrue();
        assertThat(result.msgId()).isEqualTo("MSG-001");

        final Optional<TlqMessage> received = consumer.receive(TlqChannel.REALTIME_SEND, Duration.ofSeconds(1));

        assertThat(received).isPresent();
        assertThat(received.get().getPayload()).isEqualTo("<xml>hello</xml>");
        assertThat(received.get().getMsgId()).isEqualTo("MSG-001");
    }

    @Test
    void receive_emptyQueue_shouldReturnEmpty() {
        final Optional<TlqMessage> received = consumer.receive(TlqChannel.BATCH_RECEIVE, Duration.ofMillis(50));

        assertThat(received).isEmpty();
    }

    @Test
    void subscribe_shouldCallListenerOnMessage() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<TlqMessage> captured = new AtomicReference<>();

        consumer.subscribe(TlqChannel.REALTIME_RECEIVE, msg -> {
            captured.set(msg);
            latch.countDown();
        });

        final TlqMessage msg = createMessage("MSG-002", "<xml>push</xml>", TlqChannel.REALTIME_RECEIVE);
        producer.send(msg);

        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(captured.get()).isNotNull();
        assertThat(captured.get().getMsgId()).isEqualTo("MSG-002");
        assertThat(captured.get().getPayload()).isEqualTo("<xml>push</xml>");
    }

    @Test
    void connectionFactory_connectDisconnect_shouldToggleState() {
        final InMemoryTlqConnectionFactory factory = new InMemoryTlqConnectionFactory();

        assertThat(factory.isConnected()).isFalse();

        factory.connect();
        assertThat(factory.isConnected()).isTrue();

        factory.disconnect();
        assertThat(factory.isConnected()).isFalse();
    }

    @Test
    void nodeLifecycle_loginLogout_shouldTransitionState() {
        final InMemoryNodeLifecycleManager manager = new InMemoryNodeLifecycleManager();

        assertThat(manager.getState()).isEqualTo(NodeState.UNKNOWN);

        assertThat(manager.login()).isTrue();
        assertThat(manager.getState()).isEqualTo(NodeState.ONLINE);

        manager.handleHeartbeat();
        assertThat(manager.getState()).isEqualTo(NodeState.ONLINE);

        assertThat(manager.logout()).isTrue();
        assertThat(manager.getState()).isEqualTo(NodeState.OFFLINE);
    }

    @Test
    void deadLetterHandler_shouldRecordMessages() {
        final InMemoryDeadLetterHandler handler = new InMemoryDeadLetterHandler();
        final TlqMessage msg = createMessage("MSG-DL-001", "<xml>fail</xml>", TlqChannel.BATCH_SEND);

        handler.handle(msg, "max retries exceeded");

        assertThat(handler.getDeadLetters()).hasSize(1);
        assertThat(handler.getDeadLetters().get(0).getMsgId()).isEqualTo("MSG-DL-001");
    }

    private static TlqMessage createMessage(final String msgId, final String payload, final TlqChannel channel) {
        final TlqMessageAttributes attrs = TlqMessageAttributes.forRealtime(msgId);
        return new TlqMessage(payload, attrs, channel);
    }
}
