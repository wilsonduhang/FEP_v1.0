package com.puchain.fep.transport.mock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.puchain.fep.transport.api.TlqConnectionFactory;

/**
 * In-memory {@link TlqConnectionFactory} implementation for the {@code mock} transport provider.
 *
 * <p>Simulates connection state with a volatile boolean flag. No actual network I/O is performed.
 * Only active when {@code fep.transport.provider=mock} (the default if not set).</p>
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
public class InMemoryTlqConnectionFactory implements TlqConnectionFactory {

    private static final Logger LOG = LoggerFactory.getLogger(InMemoryTlqConnectionFactory.class);

    private volatile boolean connected;

    @Override
    public void connect() {
        LOG.debug("InMemory TLQ connect");
        connected = true;
    }

    @Override
    public void disconnect() {
        LOG.debug("InMemory TLQ disconnect");
        connected = false;
    }

    @Override
    public boolean isConnected() {
        return connected;
    }
}
