package com.puchain.fep.web.dashboard.stats.controller;

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

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for {@link DashboardStatsController}.
 *
 * <p>Covers all read-only statistics endpoints.
 * See PRD v1.3 section 5.2.3 (FR-WEB-DASH-STAT).</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@AutoConfigureMockMvc
class DashboardStatsControllerTest {

    private static final String TEST_ACCOUNT = "admins";
    private static final String TEST_PASSWORD = "admin@FEP2026";
    private static final String ADMIN_USER_ID = "00000000000000000000000000000001";
    private static final String BASE_URL = "/api/v1/dashboard/stats";

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

        TestRedisConfiguration.getStore().clear();
    }

    /**
     * GET cards should return stats card data with zero values.
     *
     * @throws Exception on request failure
     */
    @Test
    void getCards_shouldReturn200WithCardData() throws Exception {
        mockMvc.perform(get(BASE_URL + "/cards")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", notNullValue()))
                .andExpect(jsonPath("$.data.todayMessageCount",
                        notNullValue()));
    }

    /**
     * GET cards should serialize {@code totalAmount} as a JSON string to
     * prevent JavaScript number precision loss for amounts exceeding
     * {@code Number.MAX_SAFE_INTEGER} (2^53 - 1).
     *
     * @throws Exception on request failure
     */
    @Test
    void getCards_shouldSerializeTotalAmountAsString() throws Exception {
        mockMvc.perform(get(BASE_URL + "/cards")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAmount").isString());
    }

    /**
     * GET trend with TODAY range should return data points.
     *
     * @throws Exception on request failure
     */
    @Test
    void getTrend_today_shouldReturn200WithPoints() throws Exception {
        mockMvc.perform(get(BASE_URL + "/trend")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("range", "TODAY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    /**
     * GET distribution should return list.
     *
     * @throws Exception on request failure
     */
    @Test
    void getDistribution_shouldReturn200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/distribution")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    /**
     * GET status-distribution should return list.
     *
     * @throws Exception on request failure
     */
    @Test
    void getStatusDistribution_shouldReturn200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/status-distribution")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }
}
