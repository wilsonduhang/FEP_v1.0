package com.puchain.fep.web.reconciliation.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.puchain.fep.web.auth.domain.CaptchaResponse;
import com.puchain.fep.web.auth.domain.LoginRequest;
import com.puchain.fep.web.auth.service.CaptchaService;
import com.puchain.fep.web.config.TestRedisConfiguration;
import com.puchain.fep.web.integration.reconciliation.ClearingInstructionRecordEntity;
import com.puchain.fep.web.integration.reconciliation.ClearingInstructionRecordRepository;
import com.puchain.fep.web.reconciliation.dto.QsInfoRequest;
import com.puchain.fep.web.reconciliation.dto.SettlementInstructionRequest;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Settlement instruction controller integration test (P2e Task 7).
 *
 * <p>Covers the 2 clearing instruction endpoints exposed under
 * {@code /api/v1/settlement/instruction}: initiate (POST) / lookup-by-platPayNo
 * (GET). See PRD v1.3 section 2138 + 5.3.2.12 (FR-WEB-RECON-DAILY).</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@AutoConfigureMockMvc
class SettlementInstructionControllerTest {

    private static final String TEST_ACCOUNT = "admins";
    private static final String TEST_PASSWORD = "admin@FEP2026";
    private static final String ADMIN_USER_ID = "00000000000000000000000000000001";
    private static final String BASE_URL = "/api/v1/settlement/instruction";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CaptchaService captchaService;

    @Autowired
    private ClearingInstructionRecordRepository repository;

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
    }

    @AfterEach
    void tearDown() {
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
     * Helper: persist a {@link ClearingInstructionRecordEntity} for read-side tests.
     */
    @Transactional
    void seedInstruction(final String platPayNo, final String qsSerialNo,
                         final String status) {
        final ClearingInstructionRecordEntity e = new ClearingInstructionRecordEntity();
        e.setInstructionId(platPayNo);
        e.setQsSerialNo(qsSerialNo);
        e.setInstructionType("NORMAL");
        e.setSettlementAmount(new BigDecimal("100.00"));
        e.setPayerAccount("6228480000000001");
        e.setPayeeAccount("6228480000000002");
        e.setInstructionStatus(status);
        e.setMessageId("MSG_TEST_" + qsSerialNo);
        e.setCreatedAt(LocalDateTime.now());
        e.setUpdatedAt(LocalDateTime.now());
        repository.save(e);
    }

    /**
     * Builds a minimal valid initiate request with one qsInfo entry.
     */
    private static SettlementInstructionRequest buildRequest(final String platPayNo,
                                                              final String qsSerialNo,
                                                              final BigDecimal amt) {
        final SettlementInstructionRequest req = new SettlementInstructionRequest();
        req.setPlatPayNo(platPayNo);
        req.setSendNodeCode("A1000143000104");
        req.setDesNodeCode("B2000456000204");
        req.setSerialNo("SN_" + platPayNo);

        final QsInfoRequest qi = new QsInfoRequest();
        qi.setQsSerialNo(qsSerialNo);
        qi.setAmt(amt);
        qi.setFkfAccNo("6228480000000001");
        qi.setSkfAccNo("6228480000000002");
        qi.setFkfAccName("深圳供应链有限公司");
        qi.setSkfAccName("上海融资保理有限公司");
        qi.setWishDate("20260427");

        req.setQsInfo(List.of(qi));
        return req;
    }

    /**
     * Initiate with a valid single qsInfo entry should produce a PENDING record.
     */
    @Test
    void initiate_validRequest_shouldCreatePendingRecord() throws Exception {
        final SettlementInstructionRequest req = buildRequest(
                "PP20260427000001", "QS20260427000001", new BigDecimal("1500.00"));

        mockMvc.perform(post(BASE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("200")))
                .andExpect(jsonPath("$.data[0].instructionId", is("PP20260427000001")))
                .andExpect(jsonPath("$.data[0].qsSerialNo", is("QS20260427000001")))
                .andExpect(jsonPath("$.data[0].instructionStatus", is("PENDING")))
                // BigDecimal serialized as string
                .andExpect(jsonPath("$.data[0].settlementAmount").isString());
    }

    /**
     * Initiate with empty qsInfo list should be rejected by Bean Validation.
     */
    @Test
    void initiate_emptyQsInfo_shouldReturnParamError() throws Exception {
        final SettlementInstructionRequest req = new SettlementInstructionRequest();
        req.setPlatPayNo("PP_BAD");
        req.setSendNodeCode("A1000143000104");
        req.setDesNodeCode("B2000456000204");
        req.setSerialNo("SN_BAD");
        req.setQsInfo(List.of());

        mockMvc.perform(post(BASE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("PARAM_4002")));
    }

    /**
     * Initiate with blank platPayNo should fail Bean Validation @NotBlank.
     */
    @Test
    void initiate_blankPlatPayNo_shouldReturnParamError() throws Exception {
        final SettlementInstructionRequest req = buildRequest(
                "", "QS_X", new BigDecimal("500.00"));

        mockMvc.perform(post(BASE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("PARAM_4002")));
    }

    /**
     * Lookup by platPayNo not found should return CLEAR_INSTRUCTION_NOT_FOUND (8604).
     */
    @Test
    void getByPlatPayNo_notFound_shouldReturnClearNotFound() throws Exception {
        mockMvc.perform(get(BASE_URL + "/PP_NOPE")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("CLEAR_8604")));
    }

    /**
     * Lookup by platPayNo with seeded records should list them.
     */
    @Test
    void getByPlatPayNo_existing_shouldReturnList() throws Exception {
        seedInstruction("PP_GET_001", "QS_001", "PENDING");
        seedInstruction("PP_GET_001", "QS_002", "SUCCESS");

        mockMvc.perform(get(BASE_URL + "/PP_GET_001")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("200")))
                .andExpect(jsonPath("$.data[0].instructionId", is("PP_GET_001")))
                .andExpect(jsonPath("$.data[0].qsSerialNo", is("QS_001")))
                .andExpect(jsonPath("$.data[1].qsSerialNo", is("QS_002")));
    }

    /**
     * P2e Task 7 v1d acceptance criteria #9: ApiResult full-5-fields envelope
     * assertion on the CLEAR_BUSINESS_RULE_VIOLATION (CLEAR_8605) path.
     *
     * <p>Triggers the service-side rule via amt=0 — which passes the DTO's
     * {@code @NotNull} (amount field is intentionally not constrained at the
     * DTO layer to delegate the positive-amount business rule to
     * {@link com.puchain.fep.processor.reconciliation.ReconciliationDiffCalculator#validateBusinessRule}).
     * Service flags one qsInfo violation, throws CLEAR_8605, and
     * {@link com.puchain.fep.common.exception.GlobalExceptionHandler#handleBusiness}
     * renders the full ApiResult envelope.</p>
     *
     * <p>Asserts all 5 ApiResult fields:
     * {@code code} / {@code message} / {@code data}=null / {@code traceId} /
     * {@code timestamp} (ISO-8601 format).</p>
     */
    @Test
    void initiate_zeroAmount_shouldEmitFullApiResultEnvelopeWithClearBusinessRule()
            throws Exception {
        final SettlementInstructionRequest req = buildRequest(
                "PP_RULE_BAD", "QS_RULE_BAD", BigDecimal.ZERO);

        mockMvc.perform(post(BASE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("CLEAR_8605")))
                .andExpect(jsonPath("$.message", notNullValue()))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.traceId").exists())
                .andExpect(jsonPath("$.timestamp",
                        matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*")));
    }

    /**
     * P3 Task 4 acceptance: PK7 fields exposed in DTO must round-trip to
     * service guard. Submitting a non-null {@code signElement} forces
     * {@link com.puchain.fep.processor.reconciliation.ClearingInstructionService}
     * to reject the request via {@code CLEAR_BUSINESS_RULE_VIOLATION}, closing
     * ADR-P2e-4 Phase 1 deviation #3 (REST path can now reach the guard).
     *
     * <p>Asserts: HTTP 400 / {@code code=CLEAR_8605} / message contains "PK7" or
     * "签名" / no clearing_instruction_records row landed.</p>
     */
    @Test
    void initiate_pk7Populated_shouldReturnClearBusinessRuleAndPersistNothing()
            throws Exception {
        final SettlementInstructionRequest req = buildRequest(
                "PP_PK7_BAD", "QS_PK7_BAD", new BigDecimal("1500.00"));
        req.setSignElement("MOCK_SIGN_ELEMENT");

        final long beforeCount = repository.count();

        mockMvc.perform(post(BASE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("CLEAR_8605")))
                .andExpect(jsonPath("$.message", containsString("PK7")));

        final long afterCount = repository.count();
        assertThat(afterCount).isEqualTo(beforeCount);
    }
}
