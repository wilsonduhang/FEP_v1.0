package com.puchain.fep.web.entquery.auth.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.puchain.fep.web.auth.domain.CaptchaResponse;
import com.puchain.fep.web.auth.domain.LoginRequest;
import com.puchain.fep.web.auth.service.CaptchaService;
import com.puchain.fep.web.config.TestRedisConfiguration;
import com.puchain.fep.web.entquery.auth.dto.AuthLetterCreateRequest;
import com.puchain.fep.web.sysmgmt.config.enterprise.dto.EnterpriseCreateRequest;
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
 * 授权书管理 Controller 集成测试。
 *
 * <p>使用全量 SpringBootTest + MockMvc，通过真实登录流程获取 JWT 令牌后
 * 访问受保护的授权书接口。覆盖 CRUD、提交、状态校验。</p>
 *
 * <p>参见 PRD v1.3 §5.4 企业信息查询管理，FR-ID FR-WEB-ENT。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@AutoConfigureMockMvc
class EntAuthLetterControllerTest {

    private static final String TEST_ACCOUNT = "admins";
    private static final String TEST_PASSWORD = "admin@FEP2026";
    private static final String ADMIN_USER_ID = "00000000000000000000000000000001";
    private static final String BASE_URL = "/api/v1/ent-query/auth-letters";
    private static final String ENTERPRISE_URL = "/api/v1/sys/config/enterprises";

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
    private String testEnterpriseId;

    /**
     * 每个测试前：设置测试账号、登录获取 JWT 令牌、创建测试企业。
     *
     * @throws Exception 请求异常
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

        // Create a test enterprise for auth letter creation
        String uniqueUsci = "91430100MA" + String.valueOf(System.nanoTime()).substring(5, 13);
        EnterpriseCreateRequest entReq = new EnterpriseCreateRequest();
        entReq.setEnterpriseName("授权书测试企业");
        entReq.setUsci(uniqueUsci);

        MvcResult entResult = mockMvc.perform(post(ENTERPRISE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(entReq)))
                .andExpect(status().isOk())
                .andReturn();

        testEnterpriseId = objectMapper.readTree(entResult.getResponse().getContentAsString())
                .at("/data/enterpriseId").asText();
    }

    /**
     * 每个测试后：恢复 admin 账号，清空模拟 Redis。
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
     * 创建纸质授权书应返回 200，letterStatus 为 DRAFT。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void createAuthLetter_shouldReturn200() throws Exception {
        AuthLetterCreateRequest request = buildRequest("PAPER");

        mockMvc.perform(post(BASE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.letterId", notNullValue()))
                .andExpect(jsonPath("$.data.letterStatus", is("DRAFT")))
                .andExpect(jsonPath("$.data.authType", is("PAPER")));
    }

    /**
     * 使用不存在的企业 ID 创建授权书应返回 404。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void createInvalidEnterprise_shouldReturn404() throws Exception {
        AuthLetterCreateRequest request = buildRequest("PAPER");
        request.setEnterpriseId("nonexistent000000000000000000001");

        mockMvc.perform(post(BASE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("BIZ_5001")));
    }

    /**
     * 提交 DRAFT 状态授权书应返回 200，letterStatus 变为 SUBMITTED。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void submitDraft_shouldReturnSubmitted() throws Exception {
        String letterId = createDraftLetter();

        mockMvc.perform(post(BASE_URL + "/" + letterId + "/submit")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.letterStatus", is("SUBMITTED")));
    }

    /**
     * 更新 DRAFT 状态授权书应返回 200。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void updateDraft_shouldReturn200() throws Exception {
        String letterId = createDraftLetter();

        AuthLetterCreateRequest updateReq = buildRequest("ELECTRONIC");
        updateReq.setAuthorizedName("更新后的被授权企业名称");

        mockMvc.perform(put(BASE_URL + "/" + letterId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.authType", is("ELECTRONIC")))
                .andExpect(jsonPath("$.data.authorizedName", is("更新后的被授权企业名称")));
    }

    /**
     * 删除 DRAFT 状态授权书应返回 200。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void deleteDraft_shouldReturn200() throws Exception {
        String letterId = createDraftLetter();

        mockMvc.perform(delete(BASE_URL + "/" + letterId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    /**
     * 删除非 DRAFT 状态授权书应返回 400。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void deleteNotDraft_shouldReturn400() throws Exception {
        String letterId = createDraftLetter();

        // Submit to move to SUBMITTED
        mockMvc.perform(post(BASE_URL + "/" + letterId + "/submit")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        // Delete should fail
        mockMvc.perform(delete(BASE_URL + "/" + letterId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("BIZ_5003")));
    }

    /**
     * 搜索授权书应返回分页列表。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void search_shouldReturn200() throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records").isArray());
    }

    /**
     * 按条件搜索（authType + letterStatus + keyword）应返回过滤结果。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void searchWithFilters_shouldReturn200() throws Exception {
        createDraftLetter();

        mockMvc.perform(get(BASE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .param("authType", "PAPER")
                        .param("letterStatus", "DRAFT")
                        .param("keyword", "91430100")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records").isArray());
    }

    /**
     * 按 ID 查询不存在的授权书应返回 404。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void getById_notFound_shouldReturn404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/nonexistentletterid00000000001")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("BIZ_5001")));
    }

    /**
     * 提交非 DRAFT 状态授权书应返回 400。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void submitNotDraft_shouldReturn400() throws Exception {
        String letterId = createDraftLetter();

        // First submit
        mockMvc.perform(post(BASE_URL + "/" + letterId + "/submit")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        // Second submit should fail
        mockMvc.perform(post(BASE_URL + "/" + letterId + "/submit")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("BIZ_5003")));
    }

    /**
     * 更新非 DRAFT 状态授权书应返回 400。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void updateLetter_notDraft_shouldReturn400() throws Exception {
        // Create + submit
        AuthLetterCreateRequest createReq = new AuthLetterCreateRequest();
        createReq.setEnterpriseId(testEnterpriseId);
        createReq.setAuthType("ELECTRONIC");
        createReq.setAuthorizedUsci("91430100MA4LUPD00X");
        createReq.setAuthorizedName("被授权目标企业");

        MvcResult result = mockMvc.perform(post(BASE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isOk())
                .andReturn();
        String letterId = objectMapper.readTree(result.getResponse().getContentAsString())
                .at("/data/letterId").asText();

        mockMvc.perform(post(BASE_URL + "/" + letterId + "/submit")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        // Try update -> 400
        AuthLetterCreateRequest updateReq = new AuthLetterCreateRequest();
        updateReq.setEnterpriseId(testEnterpriseId);
        updateReq.setAuthType("ELECTRONIC");
        updateReq.setAuthorizedUsci("91430100MA4LUPD00X");
        updateReq.setAuthorizedName("被授权目标企业");
        updateReq.setAuthScope("should fail");

        mockMvc.perform(put(BASE_URL + "/" + letterId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isBadRequest());
    }

    // ===== Helper Methods =====

    private AuthLetterCreateRequest buildRequest(final String authType) {
        AuthLetterCreateRequest request = new AuthLetterCreateRequest();
        request.setEnterpriseId(testEnterpriseId);
        request.setAuthType(authType);
        request.setAuthScope("全量查询授权");
        request.setAuthorizedUsci("91430100MA4L2YWK0T");
        request.setAuthorizedName("被授权目标企业");
        return request;
    }

    private String createDraftLetter() throws Exception {
        AuthLetterCreateRequest request = buildRequest("PAPER");

        MvcResult result = mockMvc.perform(post(BASE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .at("/data/letterId").asText();
    }
}
