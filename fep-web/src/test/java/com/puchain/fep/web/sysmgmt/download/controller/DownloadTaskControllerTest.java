package com.puchain.fep.web.sysmgmt.download.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.puchain.fep.web.auth.domain.CaptchaResponse;
import com.puchain.fep.web.auth.domain.LoginRequest;
import com.puchain.fep.web.auth.service.CaptchaService;
import com.puchain.fep.web.config.TestRedisConfiguration;
import com.puchain.fep.web.sysmgmt.download.domain.TaskType;
import com.puchain.fep.web.sysmgmt.download.dto.DownloadTaskResponse;
import com.puchain.fep.web.sysmgmt.download.service.DownloadTaskService;
import com.puchain.fep.web.sysmgmt.download.repository.SysDownloadTaskRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link DownloadTaskController} 集成测试。
 *
 * <p>使用全量 SpringBootTest + MockMvc，通过真实登录流程获取 JWT 令牌后访问受保护的
 * 下载任务管理接口。验证我的任务列表、按 ID 查询、下载未完成任务报错、删除任务功能。
 * 参见 PRD v1.3 §5.10.5 下载任务（FR-WEB-SYS-DL）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@AutoConfigureMockMvc
class DownloadTaskControllerTest {

    /** 测试登录账号（满足 LoginRequest @Size(min=6) 约束）。 */
    private static final String TEST_ACCOUNT = "admins";

    /** 测试登录密码。 */
    private static final String TEST_PASSWORD = "admin@FEP2026";

    /** 种子数据中 admin 用户 ID。 */
    private static final String ADMIN_USER_ID = "00000000000000000000000000000001";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CaptchaService captchaService;

    @Autowired
    private DownloadTaskService downloadTaskService;

    @Autowired
    private SysDownloadTaskRepository downloadTaskRepository;

    @Autowired
    private com.puchain.fep.web.sysmgmt.user.repository.SysUserRepository userRepository;

    @Autowired
    private com.puchain.fep.common.security.PasswordHasher passwordHasher;

    /** 登录获取的 JWT 令牌，供测试方法使用。 */
    private String accessToken;

    /** 保存原始账号，测试结束后恢复。 */
    private String originalAccount;

    /** 保存原始密码哈希，测试结束后恢复。 */
    private String originalPasswordHash;

    /**
     * 每个测试前：清空模拟 Redis、将 admin 账号临时改为满足 min=6 约束的账号并完成登录以获取 JWT 令牌。
     *
     * @throws Exception 登录请求异常
     */
    @BeforeEach
    void setUp() throws Exception {
        TestRedisConfiguration.getStore().clear();

        // 将种子 admin 账号临时改为满足 min=6 约束的账号
        com.puchain.fep.web.sysmgmt.user.domain.SysUser admin =
                userRepository.findById(ADMIN_USER_ID).orElseThrow();
        originalAccount = admin.getUserAccount();
        originalPasswordHash = admin.getPasswordHash();
        admin.setUserAccount(TEST_ACCOUNT);
        admin.setPasswordHash(passwordHasher.hash(TEST_PASSWORD));
        admin.setLoginFailCount(0);
        admin.setLockedUntil(null);
        userRepository.save(admin);

        // 登录获取 JWT
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
        assertFalse(accessToken.isBlank(), "accessToken 不应为空");
    }

    /**
     * 每个测试后：清理下载任务数据，恢复 admin 账号，清空模拟 Redis。
     */
    @AfterEach
    void tearDown() {
        downloadTaskRepository.deleteAll();

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
     * 我的任务列表在无任务时应返回空集合（total=0）。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void myTasks_shouldReturnEmptyList() throws Exception {
        mockMvc.perform(get("/api/v1/sys/downloads")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.total").value(0))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    /**
     * 通过 Service 创建任务后，按 ID 查询应返回正确的任务信息。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void findById_shouldReturnTask() throws Exception {
        // 通过 Service 创建任务（requesterId 与登录用户一致）
        DownloadTaskResponse created = downloadTaskService.createTask(
                "导出测试报表", TaskType.DATA_EXPORT, ADMIN_USER_ID);
        String taskId = created.getTaskId();

        MvcResult result = mockMvc.perform(get("/api/v1/sys/downloads/" + taskId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.taskId").value(taskId))
                .andExpect(jsonPath("$.data.taskName").value("导出测试报表"))
                .andExpect(jsonPath("$.data.taskStatus").value("WAITING"))
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        assertThat(data.path("taskId").asText()).isEqualTo(taskId);
    }

    /**
     * 对未完成（WAITING 状态）的任务调用下载接口，应返回业务错误（非 200 的 code 字段）。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void downloadFile_incompleteTask_shouldReturnError() throws Exception {
        // 创建 WAITING 状态任务，不调用 completeTask
        DownloadTaskResponse created = downloadTaskService.createTask(
                "未完成任务", TaskType.DATA_EXPORT, ADMIN_USER_ID);
        String taskId = created.getTaskId();

        // 下载接口应返回错误（全局异常处理器将 FepBusinessException 转换为 400 + 业务错误码）
        mockMvc.perform(get("/api/v1/sys/downloads/" + taskId + "/file")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BIZ_5003"));
    }

    /**
     * 删除任务后，再次按 ID 查询应返回任务不存在的业务错误。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void delete_shouldRemoveTask() throws Exception {
        // 先创建任务
        DownloadTaskResponse created = downloadTaskService.createTask(
                "待删除任务", TaskType.DATA_EXPORT, ADMIN_USER_ID);
        String taskId = created.getTaskId();

        // 执行删除
        mockMvc.perform(delete("/api/v1/sys/downloads/" + taskId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));

        // 再次查询应返回任务不存在错误（HTTP 400 + BIZ_5001）
        mockMvc.perform(get("/api/v1/sys/downloads/" + taskId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BIZ_5001"));
    }
}
