package com.puchain.fep.web.entquery.task.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.puchain.fep.web.auth.domain.CaptchaResponse;
import com.puchain.fep.web.auth.domain.LoginRequest;
import com.puchain.fep.web.auth.service.CaptchaService;
import com.puchain.fep.web.config.TestRedisConfiguration;
import com.puchain.fep.web.entquery.task.dto.QueryTaskCreateRequest;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 企业查询任务管理 Controller 集成测试。
 *
 * <p>使用全量 SpringBootTest + MockMvc，通过真实登录流程获取 JWT 令牌后
 * 访问受保护的查询任务接口。覆盖 CRUD、执行、状态校验。</p>
 *
 * <p>参见 PRD v1.3 §5.4 企业信息查询管理，FR-ID FR-WEB-ENT。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@AutoConfigureMockMvc
class EntQueryTaskControllerTest {

    private static final String TEST_ACCOUNT = "admins";
    private static final String TEST_PASSWORD = "admin@FEP2026";
    private static final String ADMIN_USER_ID = "00000000000000000000000000000001";
    private static final String BASE_URL = "/api/v1/ent-query/tasks";
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

        // Create a test enterprise for task creation
        String uniqueUsci = "91430100MA" + String.valueOf(System.nanoTime()).substring(5, 13);
        EnterpriseCreateRequest entReq = new EnterpriseCreateRequest();
        entReq.setEnterpriseName("查询任务测试企业");
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
     * 创建实时查询任务应返回 200，taskStatus 为 DRAFT。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void createRealtimeTask_shouldReturn200() throws Exception {
        QueryTaskCreateRequest request = buildRequest("REALTIME");

        mockMvc.perform(post(BASE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskId", notNullValue()))
                .andExpect(jsonPath("$.data.taskStatus", is("DRAFT")))
                .andExpect(jsonPath("$.data.queryType", is("REALTIME")));
    }

    /**
     * 使用不存在的企业 ID 创建任务应返回 404。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void createInvalidEnterprise_shouldReturn404() throws Exception {
        QueryTaskCreateRequest request = buildRequest("REALTIME");
        request.setEnterpriseId("nonexistent000000000000000000001");

        mockMvc.perform(post(BASE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("BIZ_5001")));
    }

    /**
     * 执行 DRAFT 状态任务应返回 200，taskStatus 变为 PROCESSING。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void executeDraft_shouldReturnProcessing() throws Exception {
        String taskId = createDraftTask();

        mockMvc.perform(post(BASE_URL + "/" + taskId + "/execute")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskStatus", is("PROCESSING")));
    }

    /**
     * 执行非 DRAFT 状态任务应返回 400。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void executeNotDraft_shouldReturn400() throws Exception {
        String taskId = createDraftTask();

        // First execute to move to PROCESSING
        mockMvc.perform(post(BASE_URL + "/" + taskId + "/execute")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        // Second execute should fail
        mockMvc.perform(post(BASE_URL + "/" + taskId + "/execute")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("BIZ_5003")));
    }

    /**
     * 删除 DRAFT 状态任务应返回 200。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void deleteDraft_shouldReturn200() throws Exception {
        String taskId = createDraftTask();

        mockMvc.perform(delete(BASE_URL + "/" + taskId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    /**
     * 删除非 DRAFT 状态任务应返回 400。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void deleteNotDraft_shouldReturn400() throws Exception {
        String taskId = createDraftTask();

        // Execute to move to PROCESSING
        mockMvc.perform(post(BASE_URL + "/" + taskId + "/execute")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        // Delete should fail
        mockMvc.perform(delete(BASE_URL + "/" + taskId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("BIZ_5003")));
    }

    /**
     * 搜索查询任务应返回分页列表。
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
     * 按条件搜索（queryType + taskStatus + keyword）应返回过滤结果。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void searchWithFilters_shouldReturn200() throws Exception {
        createDraftTask();

        mockMvc.perform(get(BASE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .param("queryType", "REALTIME")
                        .param("taskStatus", "DRAFT")
                        .param("keyword", "91430100")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records").isArray());
    }

    /**
     * 按 ID 查询不存在的任务应返回 404。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void getById_notFound_shouldReturn404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/nonexistenttaskid0000000000001")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("BIZ_5001")));
    }

    /**
     * 执行不存在的任务应返回 404。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void executeNotFound_shouldReturn404() throws Exception {
        mockMvc.perform(post(BASE_URL + "/nonexistenttaskid0000000000002/execute")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("BIZ_5001")));
    }

    /**
     * 删除不存在的任务应返回 404。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void deleteNotFound_shouldReturn404() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/nonexistenttaskid0000000000003")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("BIZ_5001")));
    }

    /**
     * USCI 为空时应返回 400 Bad Request。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void blankUsci_shouldReturn400() throws Exception {
        QueryTaskCreateRequest request = new QueryTaskCreateRequest();
        request.setEnterpriseId(testEnterpriseId);
        request.setQueryType("REALTIME");
        request.setUsci("   ");

        mockMvc.perform(post(BASE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    /**
     * 创建 BATCH 类型查询任务应返回 200，queryType 为 BATCH。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void createBatchTask_shouldReturn200() throws Exception {
        QueryTaskCreateRequest request = new QueryTaskCreateRequest();
        request.setEnterpriseId(testEnterpriseId);
        request.setQueryType("BATCH");
        request.setUsci("91430100MA4L2YWK0T");
        request.setBatchFilePath("/data/batch/query_20260407.csv");

        mockMvc.perform(post(BASE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.queryType", is("BATCH")))
                .andExpect(jsonPath("$.data.batchFilePath", is("/data/batch/query_20260407.csv")));
    }

    // ===== Helper Methods =====

    private QueryTaskCreateRequest buildRequest(final String queryType) {
        QueryTaskCreateRequest request = new QueryTaskCreateRequest();
        request.setEnterpriseId(testEnterpriseId);
        request.setQueryType(queryType);
        request.setUsci("91430100MA4L2YWK0T");
        request.setQueryTargetName("被查询目标企业");
        return request;
    }

    private String createDraftTask() throws Exception {
        QueryTaskCreateRequest request = buildRequest("REALTIME");

        MvcResult result = mockMvc.perform(post(BASE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .at("/data/taskId").asText();
    }
}
