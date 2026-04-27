package com.puchain.fep.transport.mock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.puchain.fep.transport.api.DeadLetterHandler;
import com.puchain.fep.transport.model.TlqMessage;

/**
 * In-memory {@link DeadLetterHandler} implementation for the {@code mock} transport provider.
 *
 * <p>Stores dead-letter messages in a synchronized list for inspection
 * during testing. No persistence or alerting is performed. Only active when
 * {@code fep.transport.provider=mock} (the default if not set).</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
@ConditionalOnProperty(
    name = "fep.transport.provider",
    havingValue = "mock",
    matchIfMissing = true
)
public class InMemoryDeadLetterHandler implements DeadLetterHandler {

    private static final Logger LOG = LoggerFactory.getLogger(InMemoryDeadLetterHandler.class);

    private final List<TlqMessage> deadLetters = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void handle(final TlqMessage message, final String reason) {
        LOG.debug("InMemory dead letter: msgId={}, reason={}", message.getMsgId(), reason);
        deadLetters.add(message);
    }

    /**
     * Return an unmodifiable view of recorded dead letters (for test assertions).
     *
     * @return unmodifiable list of dead-letter messages
     */
    public List<TlqMessage> getDeadLetters() {
        return Collections.unmodifiableList(deadLetters);
    }
}
