package com.puchain.fep.web.callback.http;

import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.web.submission.outputinterface.domain.SubOutputInterface;
import com.puchain.fep.web.submission.outputinterface.repository.SubOutputInterfaceRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link CallbackConnectivityProbe} 单测：加载接口委托探测 / 接口不存在抛 BIZ_5001。
 *
 * @author FEP Team
 * @since 1.0.0
 */
class CallbackConnectivityProbeTest {

    private final SubOutputInterfaceRepository repo = mock(SubOutputInterfaceRepository.class);
    private final CallbackHttpClient httpClient = mock(CallbackHttpClient.class);
    private final CallbackConnectivityProbe probe = new CallbackConnectivityProbe(repo, httpClient);

    @Test
    void probe_existingInterface_delegatesToHttpClient() {
        final SubOutputInterface iface = new SubOutputInterface();
        iface.setInterfaceId("if-1");
        when(repo.findById("if-1")).thenReturn(Optional.of(iface));
        final CallbackProbeResult expected = new CallbackProbeResult(true, 200, true, 12L, "ok");
        when(httpClient.probe(any())).thenReturn(expected);

        assertThat(probe.probe("if-1")).isEqualTo(expected);
    }

    @Test
    void probe_missingInterface_throwsBusinessException() {
        when(repo.findById("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> probe.probe("nope"))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("nope");
    }
}
