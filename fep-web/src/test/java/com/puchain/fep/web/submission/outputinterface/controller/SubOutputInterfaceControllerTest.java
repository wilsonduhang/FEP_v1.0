package com.puchain.fep.web.submission.outputinterface.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.puchain.fep.web.auth.domain.CaptchaResponse;
import com.puchain.fep.web.auth.domain.LoginRequest;
import com.puchain.fep.web.auth.service.CaptchaService;
import com.puchain.fep.web.config.TestRedisConfiguration;
import com.puchain.fep.web.submission.outputinterface.domain.InterfaceAuthType;
import com.puchain.fep.web.submission.outputinterface.dto.OutputInterfaceCreateRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Output interface controller integration test.
 *
 * <p>Covers CRUD, status toggle, connectivity test, validation, and FK checks.
 * See PRD v1.3 section 5.5.2 (FR-WEB-SUB-OUT).</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@AutoConfigureMockMvc
class SubOutputInterfaceControllerTest {

    private static final String TEST_ACCOUNT = "admins";
    private static final String TEST_PASSWORD = "admin@FEP2026";
    private static final String ADMIN_USER_ID = "00000000000000000000000000000001";
    private static final String BASE_URL = "/api/v1/submission/output-interfaces";

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
     * Create a valid output interface should return 200.
     *
     * @throws Exception on request failure
     */
    @Test
    void create_shouldPersist() throws Exception {
        OutputInterfaceCreateRequest request = buildRequest("测试输出接口A");

        mockMvc.perform(post(BASE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.interfaceId", notNullValue()))
                .andExpect(jsonPath("$.data.interfaceName", is("测试输出接口A")))
                .andExpect(jsonPath("$.data.interfaceStatus", is("ENABLED")));
    }

    /**
     * Create with blank name should return 400.
     *
     * @throws Exception on request failure
     */
    @Test
    void create_blankName_shouldReturn400() throws Exception {
        OutputInterfaceCreateRequest request = buildRequest("");
        request.setInterfaceName("");

        mockMvc.perform(post(BASE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Create with invalid URL should return 400.
     *
     * @throws Exception on request failure
     */
    @Test
    void create_invalidUrl_shouldReturn400() throws Exception {
        OutputInterfaceCreateRequest request = buildRequest("无效URL接口");
        request.setInterfaceUrl("not-a-url");

        mockMvc.perform(post(BASE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Create with nonexistent business type should return 400 (BIZ_5009).
     *
     * @throws Exception on request failure
     */
    @Test
    void create_invalidBusinessType_shouldReturn400() throws Exception {
        OutputInterfaceCreateRequest request = buildRequest("FK校验接口");
        request.setBusinessTypeId("nonexistent000000000000000000001");

        mockMvc.perform(post(BASE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("BIZ_5009")));
    }

    /**
     * Search without keyword should return paged results.
     *
     * @throws Exception on request failure
     */
    @Test
    void search_noKeyword_shouldReturnPagedResults() throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records").isArray());
    }

    /**
     * Search with keyword should filter results.
     *
     * @throws Exception on request failure
     */
    @Test
    void search_withKeyword_shouldReturnFilteredResults() throws Exception {
        createInterface("过滤搜索接口");

        mockMvc.perform(get(BASE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .param("keyword", "过滤搜索")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records").isArray());
    }

    /**
     * Get by ID should return interface details.
     *
     * @throws Exception on request failure
     */
    @Test
    void getById_shouldReturnDetails() throws Exception {
        String interfaceId = createInterface("详情输出接口");

        mockMvc.perform(get(BASE_URL + "/" + interfaceId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.interfaceName", is("详情输出接口")));
    }

    /**
     * Get nonexistent ID should return 404.
     *
     * @throws Exception on request failure
     */
    @Test
    void getById_notFound_shouldReturn404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/nonexistent000000000000000000001")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("BIZ_5001")));
    }

    /**
     * Update should modify the interface fields.
     *
     * @throws Exception on request failure
     */
    @Test
    void update_shouldModify() throws Exception {
        String interfaceId = createInterface("原接口名");

        OutputInterfaceCreateRequest updateReq = buildRequest("新接口名");
        mockMvc.perform(put(BASE_URL + "/" + interfaceId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.interfaceName", is("新接口名")));
    }

    /**
     * Toggle status should switch ENABLED to DISABLED.
     *
     * @throws Exception on request failure
     */
    @Test
    void toggleStatus_shouldSwitch() throws Exception {
        String interfaceId = createInterface("状态切换接口");

        mockMvc.perform(patch(BASE_URL + "/" + interfaceId + "/status")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.interfaceStatus", is("DISABLED")));

        // Toggle back
        mockMvc.perform(patch(BASE_URL + "/" + interfaceId + "/status")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.interfaceStatus", is("ENABLED")));
    }

    /**
     * Toggle nonexistent should return 404.
     *
     * @throws Exception on request failure
     */
    @Test
    void toggleStatus_notFound_shouldReturn404() throws Exception {
        mockMvc.perform(patch(BASE_URL + "/nonexistent000000000000000000001/status")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("BIZ_5001")));
    }

    /**
     * Delete should remove the interface.
     *
     * @throws Exception on request failure
     */
    @Test
    void delete_shouldRemove() throws Exception {
        String interfaceId = createInterface("待删除接口");

        mockMvc.perform(delete(BASE_URL + "/" + interfaceId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        mockMvc.perform(get(BASE_URL + "/" + interfaceId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    /**
     * Test connectivity on nonexistent interface should return 404.
     *
     * @throws Exception on request failure
     */
    @Test
    void testConnectivity_notFound_shouldReturn404() throws Exception {
        mockMvc.perform(post(BASE_URL + "/nonexistent000000000000000000001/test")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("BIZ_5001")));
    }

    /**
     * Test connectivity on an interface with unreachable URL should return false.
     *
     * @throws Exception on request failure
     */
    @Test
    void testConnectivity_unreachableUrl_shouldReturnFalse() throws Exception {
        String interfaceId = createInterface("不通接口");

        mockMvc.perform(post(BASE_URL + "/" + interfaceId + "/test")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", is(false)));
    }

    /**
     * Create duplicate name should return 409.
     *
     * @throws Exception on request failure
     */
    @Test
    void create_duplicateName_shouldReturn409() throws Exception {
        createInterface("重复名接口");

        OutputInterfaceCreateRequest dup = buildRequest("重复名接口");
        mockMvc.perform(post(BASE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dup)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("BIZ_5002")));
    }

    // ===== Helper Methods =====

    private OutputInterfaceCreateRequest buildRequest(final String name) {
        OutputInterfaceCreateRequest req = new OutputInterfaceCreateRequest();
        req.setInterfaceName(name);
        req.setInterfaceUrl("http://192.0.2.1:9999/api/test");
        req.setAuthType(InterfaceAuthType.NONE);
        return req;
    }

    private String createInterface(final String name) throws Exception {
        OutputInterfaceCreateRequest request = buildRequest(name);

        MvcResult result = mockMvc.perform(post(BASE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .at("/data/interfaceId").asText();
    }
}
