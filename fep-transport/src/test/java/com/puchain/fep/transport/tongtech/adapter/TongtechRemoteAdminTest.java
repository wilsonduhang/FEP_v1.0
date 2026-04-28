package com.puchain.fep.transport.tongtech.adapter;

import com.puchain.fep.transport.api.RemoteAdmin;
import com.puchain.fep.transport.model.NodeState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link TongtechRemoteAdmin}'s SDK-string mapping helper.
 *
 * <p>Full 4-stage probe flow exercising real {@code TLQConnect / TLQOptCheck /
 * TLQOptNodeSystem} requires Mockito's {@code mockConstruction} or a real broker;
 * the SDK constructors throw multiple checked exceptions and lack public no-arg
 * setters, so unit-level coverage focuses on the deterministic
 * {@link TongtechRemoteAdmin#mapStateString(String)} helper. Full IT coverage
 * is delivered by P1c-IT-bridge follow-up.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class TongtechRemoteAdminTest {

    private final TongtechRemoteAdmin admin = new TongtechRemoteAdmin(null, null);

    @Test
    @DisplayName("mapStateString: null -> UNKNOWN")
    void mapStateString_nullInput_shouldReturnUnknown() {
        assertThat(admin.mapStateString(null)).isEqualTo(NodeState.UNKNOWN);
    }

    @Test
    @DisplayName("mapStateString: RUNNING / ONLINE / ACTIVE 关键字 -> ONLINE")
    void mapStateString_onlineKeywords_shouldReturnOnline() {
        assertThat(admin.mapStateString("RUNNING")).isEqualTo(NodeState.ONLINE);
        assertThat(admin.mapStateString("running")).isEqualTo(NodeState.ONLINE);
        assertThat(admin.mapStateString("Node is ONLINE")).isEqualTo(NodeState.ONLINE);
        assertThat(admin.mapStateString("ACTIVE")).isEqualTo(NodeState.ONLINE);
    }

    @Test
    @DisplayName("mapStateString: STOPPED / OFFLINE / DOWN 关键字 -> OFFLINE")
    void mapStateString_offlineKeywords_shouldReturnOffline() {
        assertThat(admin.mapStateString("STOPPED")).isEqualTo(NodeState.OFFLINE);
        assertThat(admin.mapStateString("OFFLINE")).isEqualTo(NodeState.OFFLINE);
        assertThat(admin.mapStateString("Service down")).isEqualTo(NodeState.OFFLINE);
    }

    @Test
    @DisplayName("mapStateString: ERROR / FAULT 关键字 -> ERROR")
    void mapStateString_errorKeywords_shouldReturnError() {
        assertThat(admin.mapStateString("ERROR")).isEqualTo(NodeState.ERROR);
        assertThat(admin.mapStateString("FAULT detected")).isEqualTo(NodeState.ERROR);
    }

    @Test
    @DisplayName("mapStateString: 未识别字符串 -> UNKNOWN")
    void mapStateString_unrecognised_shouldReturnUnknown() {
        assertThat(admin.mapStateString("FOOBAR")).isEqualTo(NodeState.UNKNOWN);
        assertThat(admin.mapStateString("")).isEqualTo(NodeState.UNKNOWN);
    }

    @Test
    @DisplayName("ConnectivityProbe record: reachable / rttMs / detail accessors")
    void connectivityProbeRecord_shouldExposeAllFields() {
        RemoteAdmin.ConnectivityProbe probe =
                new RemoteAdmin.ConnectivityProbe(true, 42L, "ok");

        assertThat(probe.reachable()).isTrue();
        assertThat(probe.rttMs()).isEqualTo(42L);
        assertThat(probe.detail()).isEqualTo("ok");
    }
}
