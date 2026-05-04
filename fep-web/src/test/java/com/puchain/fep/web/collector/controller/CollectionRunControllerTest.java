package com.puchain.fep.web.collector.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.puchain.fep.common.domain.PageResult;
import com.puchain.fep.common.exception.GlobalExceptionHandler;
import com.puchain.fep.web.collector.dto.CollectionRunResponse;
import com.puchain.fep.web.collector.service.CollectionRunQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller-only test for {@link CollectionRunController} (P4 T6b).
 *
 * <p>Uses {@link MockMvcBuilders#standaloneSetup} (mirrors
 * {@code DirMapConfigControllerTest}). Validation tested with the
 * Hibernate Validator instance because standaloneSetup omits the
 * Spring auto-validator chain.</p>
 *
 * <p>Covers:
 * <ul>
 *   <li>200 OK with empty PageResult structure (records / total / pageNum / pageSize).</li>
 *   <li>200 OK with paginated results — service receives correct query DTO.</li>
 *   <li>400 BAD_REQUEST when {@code pageNum=0} (PageQuery {@code @Min(1)}).</li>
 *   <li>400 BAD_REQUEST when {@code pageSize > 100} (PageQuery {@code @Max(100)}).</li>
 * </ul>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class CollectionRunControllerTest {

    private static final String BASE_URL = "/api/v1/collector/runs";

    private final ObjectMapper om = new ObjectMapper();
    private CollectionRunQueryService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(CollectionRunQueryService.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new CollectionRunController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void search_emptyResult_returns200WithEmptyPageResult() throws Exception {
        when(service.search(any())).thenReturn(
                new PageResult<>(List.of(), 0L, 1, 20));

        mvc.perform(get(BASE_URL)
                        .param("pageNum", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("200")))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.total", is(0)))
                .andExpect(jsonPath("$.data.pageNum", is(1)))
                .andExpect(jsonPath("$.data.pageSize", is(20)));
    }

    @Test
    void search_withResults_propagatesQueryParamsToService() throws Exception {
        // Verify the controller binds query params into the request DTO,
        // distinct pageNum=2 (per feedback_pagination_adapter) avoids the
        // 1-1=0 collision that masks pagination bugs.
        CollectionRunResponse sample = new CollectionRunResponse(
                "RID_001", "ADP_3101", "SUCCESS", "MANUAL",
                Instant.parse("2026-04-30T10:00:00Z"),
                Instant.parse("2026-04-30T10:00:01Z"),
                5, 5, 5, 0, null);
        when(service.search(any())).thenReturn(
                new PageResult<>(List.of(sample), 1L, 2, 15));

        mvc.perform(get(BASE_URL)
                        .param("pageNum", "2")
                        .param("pageSize", "15")
                        .param("adapterId", "ADP_3101")
                        .param("status", "SUCCESS")
                        .param("from", "2026-04-30T00:00:00Z")
                        .param("to", "2026-04-30T23:59:59Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].runId", is("RID_001")))
                .andExpect(jsonPath("$.data.records[0].adapterId", is("ADP_3101")))
                .andExpect(jsonPath("$.data.records[0].status", is("SUCCESS")))
                .andExpect(jsonPath("$.data.pageNum", is(2)))
                .andExpect(jsonPath("$.data.pageSize", is(15)));

        // Validate the service received the bound DTO.
        ArgumentCaptor<com.puchain.fep.web.collector.dto.CollectionRunQueryRequest> cap =
                ArgumentCaptor.forClass(com.puchain.fep.web.collector.dto.CollectionRunQueryRequest.class);
        verify(service).search(cap.capture());
        assertThat(cap.getValue().getPageNum()).isEqualTo(2);
        assertThat(cap.getValue().getPageSize()).isEqualTo(15);
        assertThat(cap.getValue().getAdapterId()).isEqualTo("ADP_3101");
        assertThat(cap.getValue().getStatus()).isEqualTo("SUCCESS");
        assertThat(cap.getValue().getFrom()).isEqualTo(Instant.parse("2026-04-30T00:00:00Z"));
        assertThat(cap.getValue().getTo()).isEqualTo(Instant.parse("2026-04-30T23:59:59Z"));
    }

    @Test
    void search_invalidPageNum_returns400() throws Exception {
        mvc.perform(get(BASE_URL)
                        .param("pageNum", "0")
                        .param("pageSize", "20"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("PARAM_4002")));
    }

    @Test
    void search_pageSizeExceedsMax_returns400() throws Exception {
        mvc.perform(get(BASE_URL)
                        .param("pageNum", "1")
                        .param("pageSize", "200"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("PARAM_4002")));
    }

    @Test
    void search_defaultParams_succeed_withoutExplicitPaging() throws Exception {
        // PageQuery defaults: pageNum=1 / pageSize=20.
        when(service.search(any())).thenReturn(
                new PageResult<>(List.of(), 0L, 1, 20));

        mvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageNum", is(1)))
                .andExpect(jsonPath("$.data.pageSize", is(20)))
                .andExpect(jsonPath("$.data.total", greaterThanOrEqualTo(0)));
    }
}
