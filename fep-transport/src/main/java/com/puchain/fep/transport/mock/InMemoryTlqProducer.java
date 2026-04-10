package com.puchain.fep.transport.mock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.puchain.fep.transport.api.SendResult;
import com.puchain.fep.transport.api.TlqProducer;
import com.puchain.fep.transport.model.TlqMessage;

/**
 * In-memory {@link TlqProducer} implementation for dev profile.
 *
 * <p>Delegates to {@link InMemoryMessageBroker} and always returns a successful result.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
@Profile("dev")
public class InMemoryTlqProducer implements TlqProducer {

    private static final Logger LOG = LoggerFactory.getLogger(InMemoryTlqProducer.class);

    private final InMemoryMessageBroker broker;

    /**
     * Construct with the shared in-memory broker.
     *
     * @param broker the message broker, must not be {@code null}
     */
    public InMemoryTlqProducer(final InMemoryMessageBroker broker) {
        this.broker = broker;
    }

    @Override
    public SendResult send(final TlqMessage message) {
        LOG.debug("InMemory send: msgId={}, channel={}", message.getMsgId(), message.getChannel());
        broker.publish(message);
        return SendResult.ok(message.getMsgId());
    }
}
