package com.puchain.fep.web.entquery.result.controller;

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

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 查询结果管理 Controller 集成测试。
 *
 * <p>使用全量 SpringBootTest + MockMvc，覆盖结果列表和结果详情的
 * 正常路径与异常路径。参见 PRD v1.3 §5.4（FR-WEB-ENT）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@AutoConfigureMockMvc
class EntQueryResultControllerTest {

    private static final String TEST_ACCOUNT = "admins";
    private static final String TEST_PASSWORD = "admin@FEP2026";
    private static final String ADMIN_USER_ID = "00000000000000000000000000000001";
    private static final String TASK_BASE_URL = "/api/v1/ent-query/tasks";
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
    private String testTaskId;

    /**
     * 每个测试前：设置测试账号、登录获取 JWT 令牌、创建企业 + 查询任务。
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

        accessToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
        assertFalse(accessToken.isBlank(), "accessToken should not be blank");

        // Create enterprise
        String uniqueUsci = "91430100MA" + String.valueOf(System.nanoTime()).substring(5, 13);
        EnterpriseCreateRequest entReq = new EnterpriseCreateRequest();
        entReq.setEnterpriseName("查询结果测试企业");
        entReq.setUsci(uniqueUsci);

        MvcResult entResult = mockMvc.perform(post(ENTERPRISE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(entReq)))
                .andExpect(status().isOk())
                .andReturn();

        String enterpriseId = objectMapper.readTree(entResult.getResponse().getContentAsString())
                .at("/data/enterpriseId").asText();

        // Create query task
        QueryTaskCreateRequest taskReq = new QueryTaskCreateRequest();
        taskReq.setEnterpriseId(enterpriseId);
        taskReq.setQueryType("REALTIME");
        taskReq.setUsci("91430100MA4L2YWK0T");
        taskReq.setQueryTargetName("结果测试目标企业");

        MvcResult taskResult = mockMvc.perform(post(TASK_BASE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskReq)))
                .andExpect(status().isOk())
                .andReturn();

        testTaskId = objectMapper.readTree(taskResult.getResponse().getContentAsString())
                .at("/data/taskId").asText();
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
     * 查询已有任务的结果列表应返回 200 + 空数组（尚无回执写入结果）。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void listResults_shouldReturn200WithEmptyArray() throws Exception {
        mockMvc.perform(get(TASK_BASE_URL + "/" + testTaskId + "/results")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", empty()));
    }

    /**
     * 查询不存在任务的结果列表应返回 404。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void listResults_invalidTaskId_shouldReturn404() throws Exception {
        mockMvc.perform(get(TASK_BASE_URL + "/nonexistenttaskid0000000000099/results")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("BIZ_5001")));
    }

    /**
     * 查询不存在任务的结果详情应返回 404。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void getResult_invalidTaskId_shouldReturn404() throws Exception {
        mockMvc.perform(get(TASK_BASE_URL + "/nonexistenttaskid0000000000099/results/anyresult001")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("BIZ_5001")));
    }

    /**
     * 查询存在任务但不存在结果 ID 的详情应返回 404。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void getResult_invalidResultId_shouldReturn404() throws Exception {
        mockMvc.perform(get(TASK_BASE_URL + "/" + testTaskId + "/results/nonexistentresult00001")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("BIZ_5001")));
    }
}
