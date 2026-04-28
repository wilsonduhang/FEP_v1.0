package com.puchain.fep.transport.mock;

import com.puchain.fep.transport.api.RemoteAdmin;
import com.puchain.fep.transport.model.NodeState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link InMemoryRemoteAdmin} — verifies stub data shape so the
 * mock provider path stays deterministic for fep-web业务编排测试.
 *
 * @author FEP Team
 * @since 1.0.0
 */
class InMemoryRemoteAdminTest {

    private final InMemoryRemoteAdmin admin = new InMemoryRemoteAdmin();

    @Test
    @DisplayName("checkConnectivity 返回 reachable=true / rttMs=0 / detail='mock-stub'")
    void checkConnectivity_shouldReturnDeterministicSuccessStub() {
        RemoteAdmin.ConnectivityProbe probe = admin.checkConnectivity("any-host", 12345);

        assertThat(probe.reachable()).isTrue();
        assertThat(probe.rttMs()).isZero();
        assertThat(probe.detail()).isEqualTo("mock-stub");
    }

    @Test
    @DisplayName("getRemoteNodeState 永远返回 ONLINE")
    void getRemoteNodeState_shouldReturnOnline() {
        assertThat(admin.getRemoteNodeState("any-host", 12345))
                .isEqualTo(NodeState.ONLINE);
    }
}
