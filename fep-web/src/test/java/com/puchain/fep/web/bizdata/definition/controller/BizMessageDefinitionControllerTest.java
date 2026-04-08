package com.puchain.fep.web.bizdata.definition.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.puchain.fep.web.auth.domain.CaptchaResponse;
import com.puchain.fep.web.auth.domain.LoginRequest;
import com.puchain.fep.web.auth.service.CaptchaService;
import com.puchain.fep.web.bizdata.definition.repository.BizMessageDefinitionRepository;
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

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for {@link BizMessageDefinitionController}.
 *
 * <p>Covers CRUD and status toggle endpoints for message definitions.
 * See PRD v1.3 section 5.3.1 + section 5.3.2
 * (FR-WEB-BIZ-LIST, FR-WEB-BIZ-DICT).</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@AutoConfigureMockMvc
class BizMessageDefinitionControllerTest {

    private static final String TEST_ACCOUNT = "admins";
    private static final String TEST_PASSWORD = "admin@FEP2026";
    private static final String ADMIN_USER_ID = "00000000000000000000000000000001";
    private static final String BASE_URL = "/api/v1/bizdata/definitions";

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
    private BizMessageDefinitionRepository definitionRepository;

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
        definitionRepository.deleteAll();

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

        definitionRepository.deleteAll();
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
                "messageName", "Test Definition",
                "direction", "OUTBOUND",
                "fieldCount", 5);

        mockMvc.perform(post(BASE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.definitionId", notNullValue()))
                .andExpect(jsonPath("$.data.messageCode", is("10001")))
                .andExpect(jsonPath("$.data.definitionStatus",
                        is("ENABLED")));
    }

    /**
     * POST create with duplicate messageCode should return 400 BIZ_5011.
     *
     * @throws Exception on request failure
     */
    @Test
    void create_duplicateCode_shouldReturn400() throws Exception {
        createDefinition("20001", "First Definition");

        Map<String, Object> request = Map.of(
                "messageCode", "20001",
                "messageName", "Duplicate Definition",
                "direction", "INBOUND",
                "fieldCount", 3);

        mockMvc.perform(post(BASE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("BIZ_5011")));
    }

    /**
     * GET search should return paged results.
     *
     * @throws Exception on request failure
     */
    @Test
    void search_shouldReturnPagedResults() throws Exception {
        createDefinition("30001", "Searchable Definition");

        mockMvc.perform(get(BASE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.total",
                        greaterThanOrEqualTo(1)));
    }

    /**
     * GET by ID should return definition details.
     *
     * @throws Exception on request failure
     */
    @Test
    void getById_shouldReturnDetails() throws Exception {
        String defId = createDefinition("40001", "Detail Definition");

        mockMvc.perform(get(BASE_URL + "/" + defId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.messageName",
                        is("Detail Definition")));
    }

    /**
     * GET nonexistent definition should return 400 BIZ_5012.
     *
     * @throws Exception on request failure
     */
    @Test
    void getById_notFound_shouldReturn400() throws Exception {
        mockMvc.perform(get(BASE_URL + "/nonexistent00000000000000001")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("BIZ_5012")));
    }

    /**
     * PUT toggle-status should toggle between ENABLED and DISABLED.
     *
     * @throws Exception on request failure
     */
    @Test
    void toggleStatus_shouldToggle() throws Exception {
        String defId = createDefinition("50001", "Toggle Definition");

        mockMvc.perform(put(BASE_URL + "/" + defId + "/toggle-status")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.definitionStatus",
                        is("DISABLED")));
    }

    // ===== Helper Methods =====

    private String createDefinition(final String code,
                                    final String name) throws Exception {
        Map<String, Object> request = Map.of(
                "messageCode", code,
                "messageName", name,
                "direction", "OUTBOUND",
                "fieldCount", 5);

        MvcResult result = mockMvc.perform(post(BASE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(
                result.getResponse().getContentAsString())
                .at("/data/definitionId").asText();
    }
}
