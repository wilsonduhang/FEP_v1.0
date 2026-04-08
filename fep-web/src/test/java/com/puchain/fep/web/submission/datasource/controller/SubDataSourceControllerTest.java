package com.puchain.fep.web.submission.datasource.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.puchain.fep.web.auth.domain.CaptchaResponse;
import com.puchain.fep.web.auth.domain.LoginRequest;
import com.puchain.fep.web.auth.service.CaptchaService;
import com.puchain.fep.web.config.TestRedisConfiguration;
import com.puchain.fep.web.submission.datasource.dto.DataSourceCreateRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Data source controller integration test.
 *
 * <p>Covers CRUD endpoints, validation, and duplicate name conflict.
 * See PRD v1.3 section 5.5.3 (FR-WEB-SUB-SRC).</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@AutoConfigureMockMvc
class SubDataSourceControllerTest {

    private static final String TEST_ACCOUNT = "admins";
    private static final String TEST_PASSWORD = "admin@FEP2026";
    private static final String ADMIN_USER_ID = "00000000000000000000000000000001";
    private static final String BASE_URL = "/api/v1/submission/data-sources";

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
     * Create a valid data source should return 200 with persisted data.
     *
     * @throws Exception on request failure
     */
    @Test
    void create_shouldPersist() throws Exception {
        DataSourceCreateRequest request = buildRequest("测试数据源A");

        mockMvc.perform(post(BASE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sourceId", notNullValue()))
                .andExpect(jsonPath("$.data.sourceName", is("测试数据源A")))
                .andExpect(jsonPath("$.data.sourceStatus", is("ENABLED")));
    }

    /**
     * Create with blank name should return 400.
     *
     * @throws Exception on request failure
     */
    @Test
    void create_blankName_shouldReturn400() throws Exception {
        DataSourceCreateRequest request = buildRequest("");
        request.setSourceName("");

        mockMvc.perform(post(BASE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Create with invalid phone format should return 400.
     *
     * @throws Exception on request failure
     */
    @Test
    void create_invalidPhone_shouldReturn400() throws Exception {
        DataSourceCreateRequest request = buildRequest("无效电话数据源");
        request.setContactPhone("abc-invalid");

        mockMvc.perform(post(BASE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Search with no keyword should return paged results.
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
        createDataSource("关键字搜索源");

        mockMvc.perform(get(BASE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .param("keyword", "关键字搜索")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records").isArray());
    }

    /**
     * Get by ID should return the data source details.
     *
     * @throws Exception on request failure
     */
    @Test
    void getById_shouldReturnDetails() throws Exception {
        String sourceId = createDataSource("详情查询源");

        mockMvc.perform(get(BASE_URL + "/" + sourceId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sourceName", is("详情查询源")));
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
     * Update should modify the data source fields.
     *
     * @throws Exception on request failure
     */
    @Test
    void update_shouldModify() throws Exception {
        String sourceId = createDataSource("原数据源名");

        DataSourceCreateRequest updateReq = buildRequest("新数据源名");
        mockMvc.perform(put(BASE_URL + "/" + sourceId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sourceName", is("新数据源名")));
    }

    /**
     * Delete should remove the data source.
     *
     * @throws Exception on request failure
     */
    @Test
    void delete_shouldRemove() throws Exception {
        String sourceId = createDataSource("待删除源");

        mockMvc.perform(delete(BASE_URL + "/" + sourceId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        mockMvc.perform(get(BASE_URL + "/" + sourceId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    /**
     * Delete nonexistent should return 404.
     *
     * @throws Exception on request failure
     */
    @Test
    void delete_notFound_shouldReturn404() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/nonexistent000000000000000000001")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("BIZ_5001")));
    }

    /**
     * Create duplicate name should return 409.
     *
     * @throws Exception on request failure
     */
    @Test
    void create_duplicateName_shouldReturn409() throws Exception {
        createDataSource("重复名数据源");

        DataSourceCreateRequest dup = buildRequest("重复名数据源");
        mockMvc.perform(post(BASE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dup)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("BIZ_5002")));
    }

    // ===== Helper Methods =====

    private DataSourceCreateRequest buildRequest(final String name) {
        DataSourceCreateRequest req = new DataSourceCreateRequest();
        req.setSourceName(name);
        req.setContactAddress("长沙市岳麓区");
        req.setContactPhone("13800138000");
        return req;
    }

    private String createDataSource(final String name) throws Exception {
        DataSourceCreateRequest request = buildRequest(name);

        MvcResult result = mockMvc.perform(post(BASE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .at("/data/sourceId").asText();
    }
}
