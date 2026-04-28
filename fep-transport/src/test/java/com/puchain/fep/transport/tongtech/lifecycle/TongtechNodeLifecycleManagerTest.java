package com.puchain.fep.transport.tongtech.lifecycle;

import com.puchain.fep.transport.api.RemoteAdmin;
import com.puchain.fep.transport.model.NodeState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link TongtechNodeLifecycleManager} — verifies
 * the in-memory state machine transitions match the
 * {@link NodeState} contract (PRD §3.7).
 *
 * @author FEP Team
 * @since 1.0.0
 */
class TongtechNodeLifecycleManagerTest {

    private RemoteAdmin remoteAdmin;
    private TongtechNodeLifecycleManager lifecycle;

    @BeforeEach
    void setUp() {
        remoteAdmin = Mockito.mock(RemoteAdmin.class);
        lifecycle = new TongtechNodeLifecycleManager(remoteAdmin);
    }

    @Test
    @DisplayName("初始状态为 UNKNOWN")
    void initialState_shouldBeUnknown() {
        assertThat(lifecycle.getState()).isEqualTo(NodeState.UNKNOWN);
    }

    @Test
    @DisplayName("login: UNKNOWN -> ONLINE 成功")
    void login_unknownToOnline_shouldSucceed() {
        boolean ok = lifecycle.login();

        assertThat(ok).isTrue();
        assertThat(lifecycle.getState()).isEqualTo(NodeState.ONLINE);
    }

    @Test
    @DisplayName("login: ONLINE -> ONLINE 拒绝（非法迁移）")
    void login_alreadyOnline_shouldBeRejected() {
        lifecycle.login();
        boolean second = lifecycle.login();

        assertThat(second).isFalse();
        assertThat(lifecycle.getState()).isEqualTo(NodeState.ONLINE);
    }

    @Test
    @DisplayName("logout: ONLINE -> OFFLINE 成功")
    void logout_onlineToOffline_shouldSucceed() {
        lifecycle.login();
        boolean ok = lifecycle.logout();

        assertThat(ok).isTrue();
        assertThat(lifecycle.getState()).isEqualTo(NodeState.OFFLINE);
    }

    @Test
    @DisplayName("logout: UNKNOWN -> OFFLINE 拒绝（非法迁移）")
    void logout_fromUnknown_shouldBeRejected() {
        boolean ok = lifecycle.logout();

        assertThat(ok).isFalse();
        assertThat(lifecycle.getState()).isEqualTo(NodeState.UNKNOWN);
    }

    @Test
    @DisplayName("handleHeartbeat 不改状态")
    void handleHeartbeat_shouldNotMutateState() {
        lifecycle.login();
        lifecycle.handleHeartbeat();

        assertThat(lifecycle.getState()).isEqualTo(NodeState.ONLINE);
        Mockito.verifyNoInteractions(remoteAdmin);
    }

    @Test
    @DisplayName("注入的 RemoteAdmin 被持有，便于 follow-up heartbeat 真机校准")
    void remoteAdmin_shouldBeRetainedForFollowUp() {
        assertThat(lifecycle.remoteAdmin()).isSameAs(remoteAdmin);
    }
}
