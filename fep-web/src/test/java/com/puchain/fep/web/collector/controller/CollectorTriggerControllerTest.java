package com.puchain.fep.web.collector.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.puchain.fep.collector.scheduler.CollectorScheduler;
import com.puchain.fep.collector.support.CollectionRunResult;
import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.common.exception.GlobalExceptionHandler;
import com.puchain.fep.web.collector.dto.CollectorTriggerRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller-only test for {@link CollectorTriggerController} (P4 T6b).
 *
 * <p>Uses {@link MockMvcBuilders#standaloneSetup} (mirrors
 * {@code DirMapConfigControllerTest} pattern) — keeps the test light without
 * requiring a Spring context. {@link GlobalExceptionHandler} is attached so
 * {@code COLLECT_TRIGGER_REJECTED} surfaces as HTTP 400 in production shape.</p>
 *
 * <p>Covers Plan §T6 #6 acceptance:
 * <ul>
 *   <li>200 OK on SUCCESS path with assembled/submitted counts.</li>
 *   <li>200 OK on SKIPPED path (lock busy is normal outcome, NOT an error).</li>
 *   <li>400 BAD_REQUEST on COLLECT_TRIGGER_REJECTED (adapter not found / disabled).</li>
 *   <li>400 BAD_REQUEST on Bean Validation failure (blank adapterId).</li>
 *   <li>200 OK on PARTIAL with errorMessage propagation.</li>
 * </ul>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class CollectorTriggerControllerTest {

    private static final String BASE_URL = "/api/v1/collector/triggers";

    private final ObjectMapper om = new ObjectMapper();
    private CollectorScheduler scheduler;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        scheduler = mock(CollectorScheduler.class);
        // standaloneSetup omits Spring's auto-validator; explicit Hibernate-validated
        // LocalValidatorFactoryBean ensures @Valid @NotBlank trips a real
        // MethodArgumentNotValidException → mapped to PARAM_4002 by GlobalExceptionHandler.
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new CollectorTriggerController(scheduler))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void trigger_success_returns200WithCounts() throws Exception {
        CollectionRunResult result = new CollectionRunResult(
                "RID_TRIGGER_001", "ADP_3101",
                CollectionRunResult.Status.SUCCESS,
                3, 3, 0, null);
        when(scheduler.triggerManually("ADP_3101")).thenReturn(result);

        CollectorTriggerRequest req = new CollectorTriggerRequest();
        req.setAdapterId("ADP_3101");

        mvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("200")))
                .andExpect(jsonPath("$.data.runId", is("RID_TRIGGER_001")))
                .andExpect(jsonPath("$.data.adapterId", is("ADP_3101")))
                .andExpect(jsonPath("$.data.status", is("SUCCESS")))
                .andExpect(jsonPath("$.data.assembledCount", is(3)))
                .andExpect(jsonPath("$.data.submittedCount", is(3)))
                .andExpect(jsonPath("$.data.errorCount", is(0)));
    }

    @Test
    void trigger_skipped_returns200WithSkippedStatus() throws Exception {
        // Lock-busy SKIPPED is a normal business outcome, not an error.
        CollectionRunResult result = CollectionRunResult.skipped("ADP_3101");
        when(scheduler.triggerManually("ADP_3101")).thenReturn(result);

        CollectorTriggerRequest req = new CollectorTriggerRequest();
        req.setAdapterId("ADP_3101");

        mvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("200")))
                .andExpect(jsonPath("$.data.status", is("SKIPPED")))
                .andExpect(jsonPath("$.data.adapterId", is("ADP_3101")));
    }

    @Test
    void trigger_adapterNotFound_returns400ViaGlobalExceptionHandler() throws Exception {
        when(scheduler.triggerManually("ADP_NOPE"))
                .thenThrow(new FepBusinessException(
                        FepErrorCode.COLLECT_TRIGGER_REJECTED,
                        "adapter not found or disabled: ADP_NOPE"));

        CollectorTriggerRequest req = new CollectorTriggerRequest();
        req.setAdapterId("ADP_NOPE");

        mvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is(FepErrorCode.COLLECT_TRIGGER_REJECTED.getCode())));
    }

    @Test
    void trigger_blankAdapterId_returns400_paramValidation() throws Exception {
        CollectorTriggerRequest req = new CollectorTriggerRequest();
        req.setAdapterId("   ");

        mvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("PARAM_4002")));
    }

    @Test
    void trigger_partial_status_propagatesErrorMessage() throws Exception {
        // PARTIAL with first errorMessage — confirms errorMessage flows through DTO mapping.
        CollectionRunResult result = new CollectionRunResult(
                "RID_TRIGGER_002", "ADP_3101",
                CollectionRunResult.Status.PARTIAL,
                3, 2, 1, "assemble failed for sourceRef=SRC_42");
        when(scheduler.triggerManually("ADP_3101")).thenReturn(result);

        CollectorTriggerRequest req = new CollectorTriggerRequest();
        req.setAdapterId("ADP_3101");

        mvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("PARTIAL")))
                .andExpect(jsonPath("$.data.errorCount", is(1)))
                .andExpect(jsonPath("$.data.errorMessage", is("assemble failed for sourceRef=SRC_42")));
    }
}
