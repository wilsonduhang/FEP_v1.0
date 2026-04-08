package com.puchain.fep.web.submission.record.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.puchain.fep.web.auth.domain.CaptchaResponse;
import com.puchain.fep.web.auth.domain.LoginRequest;
import com.puchain.fep.web.auth.service.CaptchaService;
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

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Report controller integration test.
 *
 * <p>Covers submission record search, detail, manual upload, push trigger,
 * blocked records, by-type query, and trend. See PRD v1.3 section 5.6
 * (FR-WEB-REP-UPLOAD / FR-WEB-REP-LIST / FR-WEB-REP-VIEW /
 * FR-WEB-REP-PUSH).</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@AutoConfigureMockMvc
class SubReportControllerTest {

    private static final String TEST_ACCOUNT = "admins";
    private static final String TEST_PASSWORD = "admin@FEP2026";
    private static final String ADMIN_USER_ID = "00000000000000000000000000000001";
    private static final String BASE_URL = "/api/v1/report";

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

        TestRedisConfiguration.getStore().clear();
    }

    /**
     * Manual upload should create a PENDING record.
     *
     * @throws Exception on request failure
     */
    @Test
    void upload_shouldCreateRecord() throws Exception {
        mockMvc.perform(post(BASE_URL + "/upload")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("messageType", "SCF_001")
                        .param("messageName", "供应链融资报文")
                        .param("dataCount", "10")
                        .param("entryBy", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recordId", notNullValue()))
                .andExpect(jsonPath("$.data.pushStatus", is("PENDING")))
                .andExpect(jsonPath("$.data.messageType", is("SCF_001")));
    }

    /**
     * Manual upload with optional businessTypeId null should still succeed.
     *
     * @throws Exception on request failure
     */
    @Test
    void upload_noBusinessType_shouldSucceed() throws Exception {
        mockMvc.perform(post(BASE_URL + "/upload")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("messageType", "ENT_001")
                        .param("messageName", "企业查询报文")
                        .param("dataCount", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recordId", notNullValue()));
    }

    /**
     * Search records with no params should return paged results.
     *
     * @throws Exception on request failure
     */
    @Test
    void search_noParams_shouldReturnPagedResults() throws Exception {
        mockMvc.perform(get(BASE_URL + "/records")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records").isArray());
    }

    /**
     * Search records with keyword should filter results.
     *
     * @throws Exception on request failure
     */
    @Test
    void search_withKeyword_shouldReturn200() throws Exception {
        uploadRecord("SCF_002", "搜索目标报文");

        mockMvc.perform(get(BASE_URL + "/records")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("keyword", "搜索目标")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records").isArray());
    }

    /**
     * Search records with time range should return results.
     *
     * @throws Exception on request failure
     */
    @Test
    void search_withTimeRange_shouldReturn200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/records")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("startTime", "2026-01-01T00:00:00")
                        .param("endTime", "2026-12-31T23:59:59")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records").isArray());
    }

    /**
     * Get by ID should return record details.
     *
     * @throws Exception on request failure
     */
    @Test
    void getById_shouldReturnDetails() throws Exception {
        String recordId = uploadRecord("SCF_003", "详情查询报文");

        mockMvc.perform(get(BASE_URL + "/records/" + recordId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.messageName", is("详情查询报文")));
    }

    /**
     * Get nonexistent record should return 404.
     *
     * @throws Exception on request failure
     */
    @Test
    void getById_notFound_shouldReturn404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/records/nonexistent00000000000000001")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("BIZ_5001")));
    }

    /**
     * Trigger push on pending records should update status to PUSHING.
     *
     * @throws Exception on request failure
     */
    @Test
    void triggerPush_shouldUpdateToPushing() throws Exception {
        String recordId = uploadRecord("SCF_004", "待推送报文");

        mockMvc.perform(post(BASE_URL + "/push")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(recordId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].pushStatus", is("PUSHING")));
    }

    /**
     * Trigger push with no pending records should return 400.
     *
     * @throws Exception on request failure
     */
    @Test
    void triggerPush_noPending_shouldReturn400() throws Exception {
        mockMvc.perform(post(BASE_URL + "/push")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                List.of("nonexistent00000000000000001"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("BIZ_5003")));
    }

    /**
     * Get blocked records should return list.
     *
     * @throws Exception on request failure
     */
    @Test
    void getBlockedRecords_shouldReturnList() throws Exception {
        mockMvc.perform(get(BASE_URL + "/push/blocked")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    /**
     * Get records by message type should return paged results.
     *
     * @throws Exception on request failure
     */
    @Test
    void getByMessageType_shouldReturnPagedResults() throws Exception {
        uploadRecord("SCF_005", "按类型查询报文");

        mockMvc.perform(get(BASE_URL + "/records/by-type/SCF_005")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records").isArray());
    }

    /**
     * Get trend data by message type should return list.
     *
     * @throws Exception on request failure
     */
    @Test
    void getTrend_shouldReturnList() throws Exception {
        mockMvc.perform(get(BASE_URL + "/records/by-type/SCF_006/trend")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    // ===== Helper Methods =====

    private String uploadRecord(final String messageType,
                                final String messageName) throws Exception {
        MvcResult result = mockMvc.perform(post(BASE_URL + "/upload")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("messageType", messageType)
                        .param("messageName", messageName)
                        .param("dataCount", "5"))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .at("/data/recordId").asText();
    }
}
