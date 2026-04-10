package com.puchain.fep.transport.mock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.puchain.fep.transport.api.TlqConnectionFactory;

/**
 * In-memory {@link TlqConnectionFactory} implementation for dev profile.
 *
 * <p>Simulates connection state with a volatile boolean flag.
 * No actual network I/O is performed.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
@Profile("dev")
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
