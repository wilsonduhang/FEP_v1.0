package com.puchain.fep.transport.api;

import com.puchain.fep.transport.model.NodeState;

/**
 * Remote admin facade for TLQ broker connectivity probing and node-state inspection
 * (PRD v1.3 §3.7 节点工作流程 + §5.7.5 连通性测试).
 *
 * <p>This interface is the dependency seam between fep-web业务编排
 * ({@code TlqConnectivityService} / {@code TlqNodeLoginService}) and the underlying
 * TLQ admin SDK. Two implementations are provided per provider:</p>
 * <ul>
 *   <li>{@code InMemoryRemoteAdmin} (mock provider) — returns stub data so the mock
 *       transport stack can run without a real broker (matches
 *       {@code fep.transport.provider=mock}, default).</li>
 *   <li>{@code TongtechRemoteAdmin} (tongtech provider) — calls the real TongLINK/Q
 *       admin SDK ({@code TLQConnect / TLQOptCheck / TLQOptNodeSystem / TlqQCU})
 *       (matches {@code fep.transport.provider=tongtech}).</li>
 * </ul>
 *
 * <p>The two methods purposefully accept {@code host / port} instead of an opaque
 * node id — this keeps the contract free of fep-web entity types and lets the
 * transport layer probe arbitrary broker endpoints.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public interface RemoteAdmin {

    /**
     * Result of a connectivity probe.
     *
     * @param reachable {@code true} when all probe stages passed
     * @param rttMs     elapsed wall-clock milliseconds for the probe (always non-negative)
     * @param detail    short human-readable detail (e.g. {@code "OK"} or {@code "checkIP: ..."})
     */
    record ConnectivityProbe(boolean reachable, long rttMs, String detail) { }

    /**
     * Probe the connectivity of a remote TLQ broker endpoint.
     *
     * <p>Tongtech implementation runs a 4-stage check:
     * {@code TLQConnect → checkIP → checkListenPort → tlqTestLine}. Mock returns
     * an unconditional success stub.</p>
     *
     * @param host target host (broker or node IP/hostname); never {@code null}
     * @param port target port; must be a positive TCP port number
     * @return the probe result, never {@code null}
     */
    ConnectivityProbe checkConnectivity(String host, int port);

    /**
     * Inspect the remote node state via the admin SDK.
     *
     * <p>Tongtech implementation uses {@link com.puchain.fep.transport.model.NodeState}
     * keyword mapping over {@code TLQOptNodeSystem.getNodeState()} (the SDK only
     * exposes a node-system-wide state — {@code host / port} arguments are reserved
     * for future multi-node routing and currently used only for diagnostic logging).
     * Mock returns {@link NodeState#ONLINE}.</p>
     *
     * @param host target host (advisory; reserved for future multi-node routing)
     * @param port target port (advisory)
     * @return the mapped node state; {@link NodeState#UNKNOWN} on inspection failure
     */
    NodeState getRemoteNodeState(String host, int port);
}
