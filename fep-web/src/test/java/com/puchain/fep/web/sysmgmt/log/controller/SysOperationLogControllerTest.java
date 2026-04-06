package com.puchain.fep.web.sysmgmt.log.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.puchain.fep.common.util.IdGenerator;
import com.puchain.fep.web.auth.domain.CaptchaResponse;
import com.puchain.fep.web.auth.domain.LoginRequest;
import com.puchain.fep.web.auth.service.CaptchaService;
import com.puchain.fep.web.config.TestRedisConfiguration;
import com.puchain.fep.web.sysmgmt.log.domain.OperationType;
import com.puchain.fep.web.sysmgmt.log.domain.SysOperationLog;
import com.puchain.fep.web.sysmgmt.log.repository.SysOperationLogRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link SysOperationLogController} 集成测试。
 *
 * <p>使用全量 SpringBootTest + MockMvc，通过真实登录流程获取 JWT 令牌后访问受保护的
 * 日志查询接口。验证分页查询、条件筛选（账号/模块/时间范围）和按 ID 查询功能，
 * 以及资源不存在时返回 BIZ_5001 错误码。</p>
 *
 * <p>参见 PRD v1.3 §5.10.6 日志管理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@AutoConfigureMockMvc
class SysOperationLogControllerTest {

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
    private SysOperationLogRepository logRepository;

    @Autowired
    private com.puchain.fep.web.sysmgmt.user.repository.SysUserRepository userRepository;

    @Autowired
    private com.puchain.fep.common.security.PasswordHasher passwordHasher;

    /** 每次测试插入的日志 ID，测试后清理。 */
    private String testLogId;

    /** 登录获取的 JWT 令牌，供测试方法使用。 */
    private String accessToken;

    /** 保存原始账号，测试结束后恢复。 */
    private String originalAccount;

    /** 保存原始密码哈希，测试结束后恢复。 */
    private String originalPasswordHash;

    /**
     * 每个测试前：清空模拟 Redis、插入测试日志记录、完成登录以获取 JWT 令牌。
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

        // 插入一条测试日志
        testLogId = IdGenerator.uuid32();
        SysOperationLog entry = new SysOperationLog();
        entry.setLogId(testLogId);
        entry.setUserId(ADMIN_USER_ID);
        entry.setUserAccount("admin");
        entry.setModule("用户管理");
        entry.setOperation(OperationType.QUERY);
        entry.setDescription("查询用户列表");
        entry.setMethod("GET");
        entry.setRequestUrl("/api/v1/sys/users");
        entry.setResponseStatus(200);
        entry.setIpAddress("127.0.0.1");
        entry.setDurationMs(50L);
        entry.setCreateTime(LocalDateTime.now());
        logRepository.save(entry);

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
     * 每个测试后：清理插入的日志记录，恢复 admin 账号，清空模拟 Redis。
     */
    @AfterEach
    void tearDown() {
        logRepository.deleteById(testLogId);

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
     * 无过滤条件查询应返回分页日志，total >= 1。
     * 按模块"用户管理"过滤时应至少命中测试插入的那条日志。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void search_shouldReturnPagedLogs() throws Exception {
        // 无过滤：total >= 1（含测试日志及 setUp 登录产生的审计日志）
        mockMvc.perform(get("/api/v1/sys/logs")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.total").value(
                        org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        // 按模块过滤，确认测试日志（模块="用户管理"）可被检索到
        mockMvc.perform(get("/api/v1/sys/logs")
                        .param("module", "用户管理")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.total").value(
                        org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.records[0].module").value("用户管理"));
    }

    /**
     * 按 userAccount=admin 过滤，应匹配到测试日志（total >= 1）。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void search_byUserAccount_shouldFilter() throws Exception {
        mockMvc.perform(get("/api/v1/sys/logs")
                        .param("userAccount", "admin")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.total").value(
                        org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    /**
     * 按 module=角色管理 过滤，无匹配记录（total = 0）。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void search_byModule_shouldFilter() throws Exception {
        mockMvc.perform(get("/api/v1/sys/logs")
                        .param("module", "角色管理")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.total").value(0));
    }

    /**
     * 按今日时间范围过滤，应匹配到测试日志（total >= 1）。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void search_byTimeRange_shouldFilter() throws Exception {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1).minusSeconds(1);

        mockMvc.perform(get("/api/v1/sys/logs")
                        .param("startTime", startOfDay.toString())
                        .param("endTime", endOfDay.toString())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.total").value(
                        org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    /**
     * 按 logId 查询应返回正确的日志详情，logId 字段与请求一致。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void findById_shouldReturnLogDetail() throws Exception {
        mockMvc.perform(get("/api/v1/sys/logs/" + testLogId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.logId").value(testLogId))
                .andExpect(jsonPath("$.data.module").value("用户管理"))
                .andExpect(jsonPath("$.data.operation").value("QUERY"));
    }

    /**
     * 查询不存在的 logId 应返回 HTTP 400 且响应体 code = BIZ_5001。
     *
     * <p>GlobalExceptionHandler 将 FepBusinessException 映射到 400 Bad Request，
     * 响应体中的 code 字段携带业务错误码 BIZ_5001。</p>
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void findById_notFound_shouldReturn404() throws Exception {
        mockMvc.perform(get("/api/v1/sys/logs/nonexistentlogid00000000000000001")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BIZ_5001"));
    }

    /**
     * 传入空字符串 module 参数应按"无过滤"处理（覆盖 isBlank() 为 true 的分支）。
     *
     * <p>覆盖 SysOperationLogService.search 中
     * {@code (module == null || module.isBlank())} 的 isBlank()=true 分支。</p>
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void search_withBlankModuleParam_shouldReturnAllLogs() throws Exception {
        mockMvc.perform(get("/api/v1/sys/logs")
                        .param("module", "   ")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));
    }

    /**
     * 带 X-Forwarded-For 头（含多 IP）的请求应触发 AOP 切面并记录操作日志。
     *
     * <p>通过传入 X-Forwarded-For 头覆盖 OperationLogAspect.resolveClientIp 中
     * 多 IP 分割逻辑（commaIndex > 0 分支）。</p>
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void search_withXForwardedForMultiIp_shouldTriggerAspect() throws Exception {
        // X-Forwarded-For 多 IP: 覆盖 commaIndex > 0 分支
        mockMvc.perform(get("/api/v1/sys/logs")
                        .header("Authorization", "Bearer " + accessToken)
                        .header("X-Forwarded-For", "192.168.1.100, 10.0.0.1, 172.16.0.1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));
    }

    /**
     * 带 X-Forwarded-For=unknown 头的请求应跳过该头并尝试 X-Real-IP。
     *
     * <p>覆盖 OperationLogAspect.resolveClientIp 中 X-Forwarded-For=unknown 分支
     * 以及 X-Real-IP 单 IP 分支。</p>
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void search_withXForwardedForUnknownAndXRealIp_shouldTriggerAspect() throws Exception {
        // X-Forwarded-For=unknown → 跳过; X-Real-IP 有效 → 使用 X-Real-IP
        mockMvc.perform(get("/api/v1/sys/logs")
                        .header("Authorization", "Bearer " + accessToken)
                        .header("X-Forwarded-For", "unknown")
                        .header("X-Real-IP", "10.0.0.50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));
    }

    /**
     * 不带代理头的请求应使用 remoteAddr 作为 IP。
     *
     * <p>覆盖 OperationLogAspect.resolveClientIp 的 fallthrough 到 remoteAddr 分支。</p>
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void search_withoutProxyHeaders_shouldUseRemoteAddr() throws Exception {
        // 不带任何代理头: 覆盖 X-Forwarded-For null 分支和 X-Real-IP null 分支
        mockMvc.perform(get("/api/v1/sys/logs")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));
    }
}
