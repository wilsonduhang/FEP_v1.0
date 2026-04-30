package com.puchain.fep.web.sysmgmt.config.dirmap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.puchain.fep.web.sysmgmt.config.dirmap.controller.DirMapConfigController;
import com.puchain.fep.web.sysmgmt.config.dirmap.dto.DirMapConfigResponse;
import com.puchain.fep.web.sysmgmt.config.dirmap.dto.DirMapConfigUpdateRequest;
import com.puchain.fep.web.sysmgmt.config.dirmap.service.DirMapConfigAdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller-only unit test for {@link DirMapConfigController}.
 *
 * <p>Uses {@link MockMvcBuilders#standaloneSetup} rather than {@code @WebMvcTest}
 * because the latter loads {@link com.puchain.fep.web.config.JpaConfiguration}
 * which transitively requires JPA + Security beans not mockable in a slice
 * context. {@code standaloneSetup} keeps the test light (no Spring context)
 * while still exercising real {@code @RequestMapping} dispatch and Jackson
 * serialization. Plan §3.5 line 3027 specified {@code @WebMvcTest}; this
 * deviation is environment-driven (FepApplication-wide JpaConfiguration
 * sweep) and preserves the test's behavioral intent.
 *
 * @author FEP Team
 * @since 1.0.0
 */
class DirMapConfigControllerTest {

    private final ObjectMapper om = new ObjectMapper();
    private DirMapConfigAdminService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(DirMapConfigAdminService.class);
        mvc = MockMvcBuilders.standaloneSetup(new DirMapConfigController(service)).build();
    }

    @Test
    void shouldReturnFirstPageWith88Rows_whenPageSizeAtLeast88() throws Exception {
        when(service.listAll()).thenReturn(List.of(
                new DirMapConfigResponse("3001", "业务进展查询", "ACCEPTING_ORG",
                        "INBOUND_PASSIVE", true, "MODE_1", "system", Instant.now())));
        mvc.perform(get("/api/v1/sys/config/dir-map?pageNum=1&pageSize=100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                // PageResult exposes List<T> via getRecords() → "records" field (Plan
                // §3.5 line 3068 wrote "list" against an obsolete shape; current
                // PageResult.records is the canonical accessor — verified 2026-04-30).
                .andExpect(jsonPath("$.data.records[0].messageType").value("3001"));
    }

    @Test
    void shouldReturnUpdatedRow_afterPut() throws Exception {
        DirMapConfigUpdateRequest req = new DirMapConfigUpdateRequest(
                "INBOUND_PASSIVE", true, "MODE_1", "测试");
        when(service.update(eq("3001"), eq("ACCEPTING_ORG"), any())).thenReturn(
                new DirMapConfigResponse("3001", "业务进展查询", "ACCEPTING_ORG",
                        "INBOUND_PASSIVE", true, "MODE_1", "admin1", Instant.now()));
        mvc.perform(put("/api/v1/sys/config/dir-map/3001/ACCEPTING_ORG")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.direction").value("INBOUND_PASSIVE"));
    }
}
