package com.puchain.fep.web.reconciliation.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.puchain.fep.web.auth.domain.CaptchaResponse;
import com.puchain.fep.web.auth.domain.LoginRequest;
import com.puchain.fep.web.auth.service.CaptchaService;
import com.puchain.fep.web.config.TestRedisConfiguration;
import com.puchain.fep.web.integration.reconciliation.ReconciliationRecordEntity;
import com.puchain.fep.web.integration.reconciliation.ReconciliationRecordRepository;
import com.puchain.fep.web.reconciliation.dto.DailyReconciliationRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Reconciliation controller integration test (P2e Task 7).
 *
 * <p>Covers the 3 reconciliation endpoints exposed under
 * {@code /api/v1/reconciliation}: trigger-daily / detail-by-id / paged search.
 * See PRD v1.3 section 2137 + 5.3.2.13 (FR-WEB-RECON-DAILY).</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@AutoConfigureMockMvc
class ReconciliationControllerTest {

    private static final String TEST_ACCOUNT = "admins";
    private static final String TEST_PASSWORD = "admin@FEP2026";
    private static final String ADMIN_USER_ID = "00000000000000000000000000000001";
    private static final String BASE_URL = "/api/v1/reconciliation";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CaptchaService captchaService;

    @Autowired
    private ReconciliationRecordRepository repository;

    /** R1-DEFER-7: cleanup target — mirror E2E cleanTablesAndResetMocks (line 178-181). */
    @Autowired
    private com.puchain.fep.web.integration.reconciliation.ClearingInstructionRecordRepository clearingRepository;

    /** R1-DEFER-7: cleanup target — mirror E2E cleanTablesAndResetMocks (line 178-181). */
    @Autowired
    private com.puchain.fep.web.integration.processor.MessageProcessRecordJpaRepository messageProcessRecordRepository;

    @Autowired
    private com.puchain.fep.web.sysmgmt.user.repository.SysUserRepository userRepository;

    @Autowired
    private com.puchain.fep.common.security.PasswordHasher passwordHasher;

    private String accessToken;
    private String originalAccount;
    private String originalPasswordHash;

    @BeforeEach
    void setUp() throws Exception {
        TestRedisConfiguration.getStore().clear();

        com.puchain.fep.web.sysmgmt.user.domain.SysUser admin =
                userRepository.findById(ADMIN_USER_ID).orElseThrow();
        originalAccount = admin.getUserAccount();
        originalPasswordHash = admin.getPasswordHash();
        admin.setUserAccount(TEST_ACCOUNT);
        admin.setPasswordHash(passwordHasher.hash(TEST_PASSWORD));
        admin.setLoginFailCount(0);
        admin.setLockedUntil(null);
        userRepository.save(admin);

        CaptchaResponse cap = captchaService.generate();
        String captchaCode = TestRedisConfiguration.getStore()
                .get("fep:captcha:" + cap.getCaptchaId());

        LoginRequest loginReq = new LoginRequest();
        loginReq.setAccount(TEST_ACCOUNT);
        loginReq.setPassword(TEST_PASSWORD);
        loginReq.setCaptchaId(cap.getCaptchaId());
        loginReq.setCaptchaCode(captchaCode);

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        accessToken = body.path("data").path("accessToken").asText();
        assertFalse(accessToken.isBlank(), "accessToken should not be blank");

        // R1-DEFER-7: clean reconciliation tables to prevent multi-module flake
        clearingRepository.deleteAll();
        repository.deleteAll();
        messageProcessRecordRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        // R1-DEFER-7: post-cleanup mirror @BeforeEach to prevent leakage to next test class
        clearingRepository.deleteAll();
        repository.deleteAll();
        messageProcessRecordRepository.deleteAll();

        com.puchain.fep.web.sysmgmt.user.domain.SysUser admin =
                userRepository.findById(ADMIN_USER_ID).orElseThrow();
        admin.setUserAccount(originalAccount);
        admin.setPasswordHash(originalPasswordHash);
        admin.setLoginFailCount(0);
        admin.setLockedUntil(null);
        userRepository.save(admin);

        TestRedisConfiguration.getStore().clear();
    }

    /**
     * Helper: persist a {@link ReconciliationRecordEntity} for query-side tests.
     */
    @Transactional
    ReconciliationRecordEntity seed(final String id, final LocalDate date,
                                    final String messageType, final String status) {
        final ReconciliationRecordEntity e = new ReconciliationRecordEntity();
        e.setReconciliationId(id);
        e.setReconciliationDate(date);
        e.setMessageType(messageType);
        e.setSerialNo("SN_" + id);
        e.setTotalTransactionCount(10);
        e.setTotalTransactionAmount(new BigDecimal("123.45"));
        e.setActualCount(10);
        e.setReconciliationStatus(status);
        e.setDiscrepancyCount(0);
        e.setReconciliationTime(LocalDateTime.now());
        e.setCreatedAt(LocalDateTime.now());
        e.setUpdatedAt(LocalDateTime.now());
        return repository.save(e);
    }

    /**
     * Trigger daily with no existing records should return 400 with RECON_NO_INBOUND.
     */
    @Test
    void triggerDaily_noInbound_shouldReturnReconNoInbound() throws Exception {
        DailyReconciliationRequest req = new DailyReconciliationRequest();
        req.setDate("19000101");
        req.setMessageType("3116");

        mockMvc.perform(post(BASE_URL + "/daily")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("RECON_8603")));
    }

    /**
     * Trigger daily with seeded records should return the most recent one.
     */
    @Test
    void triggerDaily_withSeeded_shouldReturnRecord() throws Exception {
        seed("RC_20260427_001", LocalDate.of(2026, 4, 27), "3116", "COMPLETED");

        DailyReconciliationRequest req = new DailyReconciliationRequest();
        req.setDate("20260427");
        req.setMessageType("3116");

        mockMvc.perform(post(BASE_URL + "/daily")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("200")))
                .andExpect(jsonPath("$.data.reconciliationId", is("RC_20260427_001")))
                .andExpect(jsonPath("$.data.messageType", is("3116")))
                .andExpect(jsonPath("$.data.status", is("COMPLETED")));
    }

    /**
     * Trigger daily with malformed (non-yyyyMMdd) date should be rejected by
     * Bean Validation pattern (PARAM_4002).
     */
    @Test
    void triggerDaily_malformedDate_shouldReturnParamError() throws Exception {
        DailyReconciliationRequest req = new DailyReconciliationRequest();
        req.setDate("2026-04-27");  // ISO format violates @Pattern \\d{8}
        req.setMessageType("3116");

        mockMvc.perform(post(BASE_URL + "/daily")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("PARAM_4002")));
    }

    /**
     * GET by ID for nonexistent record should return RECON_NOT_FOUND (8601).
     */
    @Test
    void getById_notFound_shouldReturnReconNotFound() throws Exception {
        mockMvc.perform(get(BASE_URL + "/RC_NOPE_999")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("RECON_8601")))
                .andExpect(jsonPath("$.message", notNullValue()));
    }

    /**
     * GET by ID for existing record returns the detail.
     */
    @Test
    void getById_existing_shouldReturnDetail() throws Exception {
        seed("RC_20260426_007", LocalDate.of(2026, 4, 26), "3116", "DISCREPANCY");

        mockMvc.perform(get(BASE_URL + "/RC_20260426_007")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("200")))
                .andExpect(jsonPath("$.data.reconciliationId", is("RC_20260426_007")))
                .andExpect(jsonPath("$.data.status", is("DISCREPANCY")))
                // BigDecimal serialized as string to protect JS precision
                .andExpect(jsonPath("$.data.totalTransactionAmount").isString());
    }

    /**
     * Paged search should return an array of records (total may be 0 in clean DB).
     */
    @Test
    void search_emptyDb_shouldReturnPagedResultStructure() throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.pageNum", is(1)))
                .andExpect(jsonPath("$.data.pageSize", is(10)))
                .andExpect(jsonPath("$.data.total", greaterThanOrEqualTo(0)));
    }

    /**
     * Paged search with date + messageType filter narrows the result set.
     */
    @Test
    void search_withDateAndMessageType_shouldFilter() throws Exception {
        seed("RC_20260425_001", LocalDate.of(2026, 4, 25), "3116", "COMPLETED");
        seed("RC_20260425_002", LocalDate.of(2026, 4, 25), "3107", "PENDING");

        mockMvc.perform(get(BASE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .param("date", "2026-04-25")
                        .param("messageType", "3116")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].messageType", is("3116")));
    }

    /**
     * Paged search with invalid ISO date should return RECON_INVALID_DATE.
     */
    @Test
    void search_invalidDate_shouldReturnReconInvalidDate() throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .param("date", "not-a-date"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("RECON_8602")));
    }

    /**
     * ApiResult full-5-fields assertion against the RECON_NOT_FOUND path:
     * code, message, data (absent / null), traceId, timestamp ISO-8601 format.
     */
    @Test
    void getById_notFound_shouldEmitFullApiResultEnvelope() throws Exception {
        mockMvc.perform(get(BASE_URL + "/RC_NOPE_DET")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("RECON_8601")))
                .andExpect(jsonPath("$.message", notNullValue()))
                // GlobalExceptionHandler returns a failure ApiResult; the data field is
                // serialized as null (JsonInclude.ALWAYS), use jsonPath presence check.
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.traceId").exists())
                .andExpect(jsonPath("$.timestamp",
                        matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*")));
    }
}
