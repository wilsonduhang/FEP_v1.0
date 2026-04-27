package com.puchain.fep.transport.mock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.puchain.fep.transport.api.NodeLifecycleManager;
import com.puchain.fep.transport.model.NodeState;

/**
 * In-memory {@link NodeLifecycleManager} implementation for the {@code mock} transport provider.
 *
 * <p>Manages node state transitions in memory using the {@link NodeState}
 * state machine rules. No actual 9005/9006/9008 messages are sent. Only active when
 * {@code fep.transport.provider=mock} (the default if not set).</p>
 *
 * <p>State transitions are {@code synchronized} to prevent TOCTOU races
 * between the guard check and the write, which would otherwise be possible
 * with a plain {@code volatile} field.</p>
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
public class InMemoryNodeLifecycleManager implements NodeLifecycleManager {

    private static final Logger LOG = LoggerFactory.getLogger(InMemoryNodeLifecycleManager.class);

    private NodeState state = NodeState.UNKNOWN;

    @Override
    public synchronized boolean login() {
        if (!state.canTransitionTo(NodeState.ONLINE)) {
            LOG.warn("Cannot transition from {} to ONLINE", state);
            return false;
        }
        state = NodeState.ONLINE;
        LOG.debug("InMemory node login: state={}", state);
        return true;
    }

    @Override
    public synchronized boolean logout() {
        if (!state.canTransitionTo(NodeState.OFFLINE)) {
            LOG.warn("Cannot transition from {} to OFFLINE", state);
            return false;
        }
        state = NodeState.OFFLINE;
        LOG.debug("InMemory node logout: state={}", state);
        return true;
    }

    @Override
    public synchronized void handleHeartbeat() {
        LOG.debug("InMemory heartbeat: state={}", state);
    }

    @Override
    public synchronized NodeState getState() {
        return state;
    }
}
