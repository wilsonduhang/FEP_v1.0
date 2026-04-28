package com.puchain.fep.web.system.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for {@link TransportProviderController} (P1c T7 v1a).
 *
 * <p>Verifies the endpoint surfaces the configured {@code fep.transport.provider}
 * value (defaulting to {@code mock} in test config) — the frontend MockBadge
 * relies on this contract.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class TransportProviderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getProvider_returnsConfiguredValue() throws Exception {
        mockMvc.perform(get("/api/web/system/transport-provider"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.provider").value("mock"));
    }
}
