package com.puchain.fep.transport.mock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.puchain.fep.transport.api.NodeLifecycleManager;
import com.puchain.fep.transport.model.NodeState;

/**
 * In-memory {@link NodeLifecycleManager} implementation for dev profile.
 *
 * <p>Manages node state transitions in memory using the {@link NodeState}
 * state machine rules. No actual 9005/9006/9008 messages are sent.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
@Profile("dev")
public class InMemoryNodeLifecycleManager implements NodeLifecycleManager {

    private static final Logger LOG = LoggerFactory.getLogger(InMemoryNodeLifecycleManager.class);

    private volatile NodeState state = NodeState.UNKNOWN;

    @Override
    public boolean login() {
        if (!state.canTransitionTo(NodeState.ONLINE)) {
            LOG.warn("Cannot transition from {} to ONLINE", state);
            return false;
        }
        state = NodeState.ONLINE;
        LOG.debug("InMemory node login: state={}", state);
        return true;
    }

    @Override
    public boolean logout() {
        if (!state.canTransitionTo(NodeState.OFFLINE)) {
            LOG.warn("Cannot transition from {} to OFFLINE", state);
            return false;
        }
        state = NodeState.OFFLINE;
        LOG.debug("InMemory node logout: state={}", state);
        return true;
    }

    @Override
    public void handleHeartbeat() {
        LOG.debug("InMemory heartbeat: state={}", state);
    }

    @Override
    public NodeState getState() {
        return state;
    }
}
