package com.puchain.fep.web.callback.credential.controller;

import com.puchain.fep.web.callback.credential.service.CallbackCredentialAdminService;
import com.puchain.fep.web.callback.http.CallbackConnectivityProbe;
import com.puchain.fep.web.callback.http.CallbackProbeResult;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code POST /{id}/test-connectivity} 端点 web 层测试（standalone MockMvc）。
 *
 * @author FEP Team
 * @since 1.0.0
 */
class CallbackCredentialControllerConnectivityTest {

    private final CallbackCredentialAdminService service = mock(CallbackCredentialAdminService.class);
    private final CallbackConnectivityProbe probe = mock(CallbackConnectivityProbe.class);
    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new CallbackCredentialController(service, probe)).build();

    @Test
    void testConnectivity_returnsProbeResult() throws Exception {
        when(probe.probe("if-1"))
                .thenReturn(new CallbackProbeResult(true, 200, true, 15L, "ok"));

        mvc.perform(post("/api/v1/callback/credentials/if-1/test-connectivity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reachable").value(true))
                .andExpect(jsonPath("$.data.statusCode").value(200))
                .andExpect(jsonPath("$.data.authApplied").value(true))
                .andExpect(jsonPath("$.data.message").value("ok"));
    }
}
