package com.puchain.fep.transport.tongtech.lifecycle;

import com.puchain.fep.transport.api.NodeLifecycleManager;
import com.puchain.fep.transport.api.RemoteAdmin;
import com.puchain.fep.transport.model.NodeState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Tongtech (real TLQ SDK) {@link NodeLifecycleManager} — state machine only
 * (P1c T7 v1a / PRD v1.3 §3.7 节点工作流程).
 *
 * <p>Decoupled from message wiring: 9006 / 9008 报文由 fep-web
 * {@code TlqNodeLoginService} 编排发送；本类只负责 in-memory 状态机
 * (UNKNOWN ↔ ONLINE ↔ OFFLINE) + 守护非法迁移 + 心跳日志。</p>
 *
 * <p>{@link RemoteAdmin} 注入是为了 P1c follow-up（heartbeat 调用真 SDK
 * 校准状态），当前仅持有引用以便测试装配（依赖反应了"lifecycle 受 admin
 * SDK 控制"的语义）。</p>
 *
 * <p>State transitions are {@code synchronized} to prevent TOCTOU races
 * between the guard check and the write — same pattern as
 * {@link com.puchain.fep.transport.mock.InMemoryNodeLifecycleManager}.</p>
 *
 * <p>Active when {@code fep.transport.provider=tongtech}.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
@ConditionalOnProperty(name = "fep.transport.provider", havingValue = "tongtech")
public class TongtechNodeLifecycleManager implements NodeLifecycleManager {

    private static final Logger LOG = LoggerFactory.getLogger(TongtechNodeLifecycleManager.class);

    /** Current node state — guarded by intrinsic lock; volatile not enough due to compound check-and-set. */
    private NodeState state = NodeState.UNKNOWN;

    /** Remote admin used for future heartbeat state inspection (kept as final field for DI consistency). */
    @SuppressWarnings("PMD.SingularField")
    private final RemoteAdmin remoteAdmin;

    /**
     * Construct with the {@link RemoteAdmin} bean (Tongtech-backed).
     *
     * @param remoteAdmin the active {@link RemoteAdmin} implementation
     *                    (typically {@code TongtechRemoteAdmin})
     */
    public TongtechNodeLifecycleManager(final RemoteAdmin remoteAdmin) {
        this.remoteAdmin = remoteAdmin;
    }

    /**
     * Transition to {@link NodeState#ONLINE} after fep-web已成功发送 9006 报文。
     *
     * @return {@code true} when the transition succeeded; {@code false} when the
     *         current state forbids the transition (state machine guard)
     */
    @Override
    public synchronized boolean login() {
        if (state.canTransitionTo(NodeState.ONLINE)) {
            state = NodeState.ONLINE;
            LOG.info("Tongtech node state transitioned: -> ONLINE");
            return true;
        }
        LOG.warn("Cannot transition from {} to ONLINE", state);
        return false;
    }

    /**
     * Transition to {@link NodeState#OFFLINE} after fep-web已成功发送 9008 报文。
     *
     * @return {@code true} when the transition succeeded; {@code false} when the
     *         current state forbids the transition
     */
    @Override
    public synchronized boolean logout() {
        if (state.canTransitionTo(NodeState.OFFLINE)) {
            state = NodeState.OFFLINE;
            LOG.info("Tongtech node state transitioned: -> OFFLINE");
            return true;
        }
        LOG.warn("Cannot transition from {} to OFFLINE", state);
        return false;
    }

    /**
     * Heartbeat hook — currently a no-op other than recording the current state.
     *
     * <p>P1c-IT-bridge follow-up may evolve this to call
     * {@link RemoteAdmin#getRemoteNodeState(String, int)} and refresh the state.</p>
     */
    @Override
    public synchronized void handleHeartbeat() {
        LOG.info("Tongtech heartbeat received, current state={}", state);
    }

    /**
     * Return the current state.
     *
     * @return the current {@link NodeState}, never {@code null}
     */
    @Override
    public synchronized NodeState getState() {
        return state;
    }

    /**
     * Return the injected {@link RemoteAdmin} (package-visible for tests).
     *
     * @return the {@link RemoteAdmin} bean handed in via constructor
     */
    RemoteAdmin remoteAdmin() {
        return remoteAdmin;
    }
}
