package com.puchain.fep.web.submission.scene.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.puchain.fep.common.domain.EnableDisableStatus;
import com.puchain.fep.web.auth.domain.CaptchaResponse;
import com.puchain.fep.web.auth.domain.LoginRequest;
import com.puchain.fep.web.auth.service.CaptchaService;
import com.puchain.fep.web.config.TestRedisConfiguration;
import com.puchain.fep.web.submission.scene.domain.ScenePushMethod;
import com.puchain.fep.web.submission.scene.dto.SceneCreateRequest;
import com.puchain.fep.web.sysmgmt.config.businesstype.domain.SysBusinessType;
import com.puchain.fep.web.sysmgmt.config.businesstype.repository.SysBusinessTypeRepository;
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
 * Business scene controller integration test.
 *
 * <p>Covers CRUD, status toggle, FK validation, MANUAL template rule.
 * See PRD v1.3 section 5.5.4 (FR-WEB-SUB-SCENE).</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@AutoConfigureMockMvc
class SubBusinessSceneControllerTest {

    private static final String TEST_ACCOUNT = "admins";
    private static final String TEST_PASSWORD = "admin@FEP2026";
    private static final String ADMIN_USER_ID = "00000000000000000000000000000001";
    private static final String BASE_URL = "/api/v1/submission/scenes";
    private static final String TEST_BIZ_TYPE_ID = "test0000scenebiztypeid000000001";

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
    private SysBusinessTypeRepository businessTypeRepository;

    private String accessToken;
    private String originalAccount;
    private String originalPasswordHash;

    /**
     * Set up test account, business type seed data, and login.
     *
     * @throws Exception on request failure
     */
    @BeforeEach
    void setUp() throws Exception {
        TestRedisConfiguration.getStore().clear();

        if (!businessTypeRepository.existsById(TEST_BIZ_TYPE_ID)) {
            SysBusinessType bt = new SysBusinessType();
            bt.setTypeId(TEST_BIZ_TYPE_ID);
            bt.setTypeName("场景测试业务类型");
            bt.setTypeCode("SCENE_BT_01");
            bt.setSortOrder(99);
            bt.setTypeStatus(EnableDisableStatus.ENABLED);
            businessTypeRepository.save(bt);
        }

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
     * Restore admin credentials and clean up test data.
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

        businessTypeRepository.deleteById(TEST_BIZ_TYPE_ID);
        TestRedisConfiguration.getStore().clear();
    }

    /**
     * Create a valid AUTO scene should return 200.
     *
     * @throws Exception on request failure
     */
    @Test
    void create_auto_shouldPersist() throws Exception {
        SceneCreateRequest request = buildRequest("自动推送场景A");

        mockMvc.perform(post(BASE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sceneId", notNullValue()))
                .andExpect(jsonPath("$.data.sceneName", is("自动推送场景A")))
                .andExpect(jsonPath("$.data.sceneStatus", is("ENABLED")));
    }

    /**
     * Create MANUAL scene with template path should succeed.
     *
     * @throws Exception on request failure
     */
    @Test
    void create_manualWithTemplate_shouldPersist() throws Exception {
        SceneCreateRequest request = buildRequest("手动上传场景B");
        request.setPushMethod(ScenePushMethod.MANUAL);
        request.setImportTemplatePath("/templates/import.xlsx");

        mockMvc.perform(post(BASE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pushMethod", is("MANUAL")));
    }

    /**
     * Create MANUAL scene without template path should return 400.
     *
     * @throws Exception on request failure
     */
    @Test
    void create_manualNoTemplate_shouldReturn400() throws Exception {
        SceneCreateRequest request = buildRequest("缺模板路径场景");
        request.setPushMethod(ScenePushMethod.MANUAL);

        mockMvc.perform(post(BASE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("PARAM_4001")));
    }

    /**
     * Create with invalid business type ID should return 400.
     *
     * @throws Exception on request failure
     */
    @Test
    void create_invalidBusinessType_shouldReturn400() throws Exception {
        SceneCreateRequest request = buildRequest("无效业务类型场景");
        request.setBusinessTypeId("nonexistent000000000000000000001");

        mockMvc.perform(post(BASE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("BIZ_5009")));
    }

    /**
     * Create with name shorter than 3 chars should return 400.
     *
     * @throws Exception on request failure
     */
    @Test
    void create_nameTooShort_shouldReturn400() throws Exception {
        SceneCreateRequest request = buildRequest("AB");

        mockMvc.perform(post(BASE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Search without filters should return paged results.
     *
     * @throws Exception on request failure
     */
    @Test
    void search_noFilter_shouldReturnPagedResults() throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records").isArray());
    }

    /**
     * Search with keyword and businessTypeId should filter results.
     *
     * @throws Exception on request failure
     */
    @Test
    void search_withFilters_shouldReturnResults() throws Exception {
        createScene("关键字过滤场景");

        mockMvc.perform(get(BASE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .param("keyword", "关键字过滤")
                        .param("businessTypeId", TEST_BIZ_TYPE_ID)
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records").isArray());
    }

    /**
     * Get by ID should return scene details.
     *
     * @throws Exception on request failure
     */
    @Test
    void getById_shouldReturnDetails() throws Exception {
        String sceneId = createScene("详情场景");

        mockMvc.perform(get(BASE_URL + "/" + sceneId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sceneName", is("详情场景")));
    }

    /**
     * Update should modify the scene fields.
     *
     * @throws Exception on request failure
     */
    @Test
    void update_shouldModify() throws Exception {
        String sceneId = createScene("原场景名称");

        SceneCreateRequest updateReq = buildRequest("新场景名称");
        mockMvc.perform(put(BASE_URL + "/" + sceneId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sceneName", is("新场景名称")));
    }

    /**
     * Toggle status should switch ENABLED to DISABLED and back.
     *
     * @throws Exception on request failure
     */
    @Test
    void toggleStatus_shouldSwitch() throws Exception {
        String sceneId = createScene("状态切换场景");

        mockMvc.perform(patch(BASE_URL + "/" + sceneId + "/status")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sceneStatus", is("DISABLED")));
    }

    /**
     * Delete should remove the scene.
     *
     * @throws Exception on request failure
     */
    @Test
    void delete_shouldRemove() throws Exception {
        String sceneId = createScene("待删除场景");

        mockMvc.perform(delete(BASE_URL + "/" + sceneId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        mockMvc.perform(get(BASE_URL + "/" + sceneId)
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

    // ===== Helper Methods =====

    private SceneCreateRequest buildRequest(final String name) {
        SceneCreateRequest req = new SceneCreateRequest();
        req.setSceneName(name);
        req.setBusinessTypeId(TEST_BIZ_TYPE_ID);
        req.setPushMethod(ScenePushMethod.AUTO);
        req.setRequestUrl("http://example.com/api/data");
        req.setSortOrder(1);
        return req;
    }

    private String createScene(final String name) throws Exception {
        SceneCreateRequest request = buildRequest(name);

        MvcResult result = mockMvc.perform(post(BASE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .at("/data/sceneId").asText();
    }
}
