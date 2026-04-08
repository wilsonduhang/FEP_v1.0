package com.puchain.fep.web.dashboard.shortcut.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.puchain.fep.web.auth.domain.CaptchaResponse;
import com.puchain.fep.web.auth.domain.LoginRequest;
import com.puchain.fep.web.auth.service.CaptchaService;
import com.puchain.fep.web.config.TestRedisConfiguration;
import com.puchain.fep.web.dashboard.shortcut.repository.DashboardShortcutRepository;
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
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for {@link DashboardShortcutController}.
 *
 * <p>Covers CRUD, reorder, and visibility toggle endpoints.
 * See PRD v1.3 section 5.2.4 (FR-WEB-DASH-QUICK).</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@AutoConfigureMockMvc
class DashboardShortcutControllerTest {

    private static final String TEST_ACCOUNT = "admins";
    private static final String TEST_PASSWORD = "admin@FEP2026";
    private static final String ADMIN_USER_ID = "00000000000000000000000000000001";
    private static final String BASE_URL = "/api/v1/dashboard/shortcuts";

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
    private DashboardShortcutRepository shortcutRepository;

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
        shortcutRepository.deleteAll();

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

        shortcutRepository.deleteAll();
        TestRedisConfiguration.getStore().clear();
    }

    /**
     * POST create + GET list flow should work.
     *
     * @throws Exception on request failure
     */
    @Test
    void createAndList_shouldWork() throws Exception {
        createShortcut("Test Shortcut", "/test/url");

        mockMvc.perform(get(BASE_URL)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].shortcutName",
                        is("Test Shortcut")));
    }

    /**
     * POST create with duplicate name should return 409 BIZ_5002.
     *
     * @throws Exception on request failure
     */
    @Test
    void create_duplicateName_shouldReturn409() throws Exception {
        createShortcut("Duplicate Name", "/url/one");

        Map<String, Object> request = Map.of(
                "shortcutName", "Duplicate Name",
                "targetUrl", "/url/two");

        mockMvc.perform(post(BASE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("BIZ_5002")));
    }

    /**
     * PUT toggle-visibility should toggle visible flag.
     *
     * @throws Exception on request failure
     */
    @Test
    void toggleVisibility_shouldToggle() throws Exception {
        String shortcutId = createShortcut("Toggle Shortcut", "/toggle");

        mockMvc.perform(put(BASE_URL + "/" + shortcutId
                        + "/toggle-visibility")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.visible", is(false)));
    }

    /**
     * PUT reorder should update sort orders.
     *
     * @throws Exception on request failure
     */
    @Test
    void reorder_shouldUpdateSortOrders() throws Exception {
        String id1 = createShortcut("Shortcut A", "/url-a");
        String id2 = createShortcut("Shortcut B", "/url-b");

        Map<String, Object> reorderRequest = Map.of(
                "items", List.of(
                        Map.of("shortcutId", id1, "sortOrder", 2),
                        Map.of("shortcutId", id2, "sortOrder", 1)));

        mockMvc.perform(put(BASE_URL + "/reorder")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                reorderRequest)))
                .andExpect(status().isOk());
    }

    /**
     * DELETE should remove shortcut.
     *
     * @throws Exception on request failure
     */
    @Test
    void delete_shouldRemoveShortcut() throws Exception {
        String shortcutId = createShortcut("Delete Me", "/delete");

        mockMvc.perform(delete(BASE_URL + "/" + shortcutId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    // ===== Helper Methods =====

    private String createShortcut(final String name,
                                  final String url) throws Exception {
        Map<String, Object> request = Map.of(
                "shortcutName", name,
                "targetUrl", url);

        MvcResult result = mockMvc.perform(post(BASE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.shortcutId", notNullValue()))
                .andReturn();

        return objectMapper.readTree(
                result.getResponse().getContentAsString())
                .at("/data/shortcutId").asText();
    }
}
