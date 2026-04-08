package com.puchain.fep.web.bizdata.record.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.puchain.fep.web.auth.domain.CaptchaResponse;
import com.puchain.fep.web.auth.domain.LoginRequest;
import com.puchain.fep.web.auth.service.CaptchaService;
import com.puchain.fep.web.bizdata.record.repository.BizMessageRecordRepository;
import com.puchain.fep.web.config.TestRedisConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for {@link BizMessageRecordController}.
 *
 * <p>Covers CRUD, resubmit, search, and summary endpoints.
 * See PRD v1.3 section 5.3.1 (FR-WEB-BIZ-LIST).</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@AutoConfigureMockMvc
class BizMessageRecordControllerTest {

    private static final String TEST_ACCOUNT = "admins";
    private static final String TEST_PASSWORD = "admin@FEP2026";
    private static final String ADMIN_USER_ID = "00000000000000000000000000000001";
    private static final String BASE_URL = "/api/v1/bizdata/records";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CaptchaService captchaService;

    @Autowired
    private com.puchain.fep.web.sysmgmt.user.repository.SysUserRepository userRepository;

    @Autowired
    private com.puchain.fep.common.security.PasswordHasher passwordHasher;

    @Autowired
    private BizMessageRecordRepository recordRepository;

    private String accessToken;
    private String originalAccount;
    private String originalPasswordHash;

    /**
     * Set up test account and login to obtain JWT token.
     *
     * @throws Exception on request failure
     */
    @BeforeEach
    void setUp() throws Exception {
        TestRedisConfiguration.getStore().clear();
        recordRepository.deleteAll();

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

        JsonNode body = objectMapper.readTree(
                loginResult.getResponse().getContentAsString());
        accessToken = body.path("data").path("accessToken").asText();
        assertFalse(accessToken.isBlank(), "accessToken should not be blank");
    }

    /**
     * Restore original admin credentials and clear Redis store.
     */
    @AfterEach
    void tearDown() {
        com.puchain.fep.web.sysmgmt.user.domain.SysUser admin =
                userRepository.findById(ADMIN_USER_ID).orElseThrow();
        admin.setUserAccount(originalAccount);
        admin.setPasswordHash(originalPasswordHash);
        admin.setLoginFailCount(0);
        admin.setLockedUntil(null);
        userRepository.save(admin);

        recordRepository.deleteAll();
        TestRedisConfiguration.getStore().clear();
    }

    /**
     * POST create with valid request should return 200.
     *
     * @throws Exception on request failure
     */
    @Test
    void create_validRequest_shouldReturn200() throws Exception {
        Map<String, Object> request = Map.of(
                "messageCode", "10001",
                "serialNo", "SN-20260408-001",
                "direction", "OUTBOUND");

        mockMvc.perform(post(BASE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recordId", notNullValue()))
                .andExpect(jsonPath("$.data.processStatus",
                        is("PENDING")));
    }

    /**
     * POST create with duplicate serialNo should return 409 BIZ_5002.
     *
     * @throws Exception on request failure
     */
    @Test
    void create_duplicateSerialNo_shouldReturn409() throws Exception {
        createRecord("10002", "SN-DUP-001");

        Map<String, Object> request = Map.of(
                "messageCode", "10003",
                "serialNo", "SN-DUP-001",
                "direction", "INBOUND");

        mockMvc.perform(post(BASE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("BIZ_5002")));
    }

    /**
     * GET search should return paged results.
     *
     * @throws Exception on request failure
     */
    @Test
    void search_shouldReturnPagedResults() throws Exception {
        createRecord("20001", "SN-SEARCH-001");

        mockMvc.perform(get(BASE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .param("pageNum", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records").isArray());
    }

    /**
     * GET summary should return empty list when no data.
     *
     * @throws Exception on request failure
     */
    @Test
    void getSummary_empty_shouldReturn200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/summary")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    /**
     * GET by ID should return record details.
     *
     * @throws Exception on request failure
     */
    @Test
    void getById_shouldReturnDetails() throws Exception {
        String recordId = createRecord("30001", "SN-DETAIL-001");

        mockMvc.perform(get(BASE_URL + "/" + recordId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.serialNo",
                        is("SN-DETAIL-001")));
    }

    /**
     * POST resubmit on non-FAILED record should return 400 BIZ_5003.
     *
     * @throws Exception on request failure
     */
    @Test
    void resubmit_notFailed_shouldReturn400() throws Exception {
        String recordId = createRecord("40001", "SN-RESUB-001");

        mockMvc.perform(post(BASE_URL + "/" + recordId + "/resubmit")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("BIZ_5003")));
    }

    // ===== Helper Methods =====

    private String createRecord(final String messageCode,
                                final String serialNo) throws Exception {
        Map<String, Object> request = Map.of(
                "messageCode", messageCode,
                "serialNo", serialNo,
                "direction", "OUTBOUND");

        MvcResult result = mockMvc.perform(post(BASE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(
                result.getResponse().getContentAsString())
                .at("/data/recordId").asText();
    }
}
