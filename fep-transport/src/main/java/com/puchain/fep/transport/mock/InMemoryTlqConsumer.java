package com.puchain.fep.transport.mock;

import java.time.Duration;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.puchain.fep.transport.api.MessageListener;
import com.puchain.fep.transport.api.TlqConsumer;
import com.puchain.fep.transport.model.TlqChannel;
import com.puchain.fep.transport.model.TlqMessage;

/**
 * In-memory {@link TlqConsumer} implementation for dev profile.
 *
 * <p>Supports both pull mode ({@link #receive}) and push mode
 * ({@link #subscribe}/{@link #unsubscribe}) via {@link InMemoryMessageBroker}.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
@Profile("dev")
public class InMemoryTlqConsumer implements TlqConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(InMemoryTlqConsumer.class);

    private final InMemoryMessageBroker broker;

    /**
     * Construct with the shared in-memory broker.
     *
     * @param broker the message broker, must not be {@code null}
     */
    public InMemoryTlqConsumer(final InMemoryMessageBroker broker) {
        this.broker = broker;
    }

    @Override
    public Optional<TlqMessage> receive(final TlqChannel channel, final Duration timeout) {
        LOG.debug("InMemory receive: channel={}, timeout={}ms", channel, timeout.toMillis());
        final TlqMessage msg = broker.poll(channel, timeout.toMillis());
        return Optional.ofNullable(msg);
    }

    @Override
    public void subscribe(final TlqChannel channel, final MessageListener listener) {
        LOG.debug("InMemory subscribe: channel={}", channel);
        broker.addListener(channel, listener);
    }

    @Override
    public void unsubscribe(final TlqChannel channel) {
        LOG.debug("InMemory unsubscribe: channel={}", channel);
        broker.removeListener(channel);
    }
}
