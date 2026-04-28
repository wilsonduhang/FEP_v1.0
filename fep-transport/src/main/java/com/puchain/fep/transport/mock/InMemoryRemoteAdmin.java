package com.puchain.fep.transport.mock;

import com.puchain.fep.transport.api.RemoteAdmin;
import com.puchain.fep.transport.model.NodeState;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * In-memory {@link RemoteAdmin} implementation for the {@code mock} transport provider
 * (P1c T7 / PRD §3.7 + §5.7.5).
 *
 * <p>Returns deterministic stub data so the mock transport stack — used in
 * unit tests, integration tests without a broker, and local dev — can satisfy
 * fep-web's {@code TlqConnectivityService} / {@code TlqNodeLoginService}
 * dependencies without any real network I/O.</p>
 *
 * <p>Active when {@code fep.transport.provider=mock} (the default if the
 * property is missing).</p>
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
public class InMemoryRemoteAdmin implements RemoteAdmin {

    /** Stub detail string surfaced to {@code ConnectivityTestResponse.message}. */
    static final String STUB_DETAIL = "mock-stub";

    /**
     * Always reports the endpoint reachable with zero RTT.
     *
     * @param host target host (ignored)
     * @param port target port (ignored)
     * @return a stub probe with {@code reachable=true, rttMs=0, detail="mock-stub"}
     */
    @Override
    public ConnectivityProbe checkConnectivity(final String host, final int port) {
        return new ConnectivityProbe(true, 0L, STUB_DETAIL);
    }

    /**
     * Always reports the remote node as {@link NodeState#ONLINE}.
     *
     * @param host target host (ignored)
     * @param port target port (ignored)
     * @return {@link NodeState#ONLINE}
     */
    @Override
    public NodeState getRemoteNodeState(final String host, final int port) {
        return NodeState.ONLINE;
    }
}
