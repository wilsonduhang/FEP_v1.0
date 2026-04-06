package com.puchain.fep.web.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.puchain.fep.common.security.PasswordHasher;
import com.puchain.fep.web.auth.domain.CaptchaResponse;
import com.puchain.fep.web.auth.domain.LoginRequest;
import com.puchain.fep.web.auth.service.CaptchaService;
import com.puchain.fep.web.config.TestRedisConfiguration;
import com.puchain.fep.web.sysmgmt.user.domain.SysUser;
import com.puchain.fep.web.sysmgmt.user.repository.SysUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RBAC 端到端流程集成测试。
 *
 * <p>验证完整流程: 验证码 -> 登录 -> 受保护接口访问 -> 菜单树 -> 登出 -> 令牌失效。
 * 另含单点踢出 (SSO) 场景: 第二次登录使第一次令牌失效 (PRD SS5.1.5)。</p>
 *
 * <p>使用 {@link TestRedisConfiguration} 提供的 ConcurrentHashMap 支撑的
 * Mock StringRedisTemplate，无需外部 Redis 实例。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RbacFlowTest {

    /** 测试用登录账号（满足 LoginRequest @Size(min=6) 约束）。 */
    private static final String TEST_ACCOUNT = "admins";

    /** 测试用密码。 */
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
    private SysUserRepository userRepository;

    @Autowired
    private PasswordHasher passwordHasher;

    /** 保存原始账号，测试结束后恢复。 */
    private String originalAccount;

    /** 保存原始密码哈希，测试结束后恢复。 */
    private String originalPasswordHash;

    /**
     * 每个测试前: 清空模拟 Redis、重置 admin 账号和密码。
     */
    @BeforeEach
    void setUp() {
        // 清空模拟 Redis
        TestRedisConfiguration.getStore().clear();

        // 保存原始值以便恢复
        SysUser admin = userRepository.findById(ADMIN_USER_ID).orElseThrow();
        originalAccount = admin.getUserAccount();
        originalPasswordHash = admin.getPasswordHash();

        // 设置测试用账号和密码
        admin.setUserAccount(TEST_ACCOUNT);
        admin.setPasswordHash(passwordHasher.hash(TEST_PASSWORD));
        admin.setLoginFailCount(0);
        admin.setLockedUntil(null);
        userRepository.save(admin);
    }

    /**
     * 每个测试后: 恢复原始 admin 账号数据，避免影响其他测试。
     */
    @AfterEach
    void tearDown() {
        SysUser admin = userRepository.findById(ADMIN_USER_ID).orElseThrow();
        admin.setUserAccount(originalAccount);
        admin.setPasswordHash(originalPasswordHash);
        admin.setLoginFailCount(0);
        admin.setLockedUntil(null);
        userRepository.save(admin);
        TestRedisConfiguration.getStore().clear();
    }

    /**
     * 完整管理员流程: 登录 -> 访问用户列表 -> 查询菜单树 -> 登出 -> 令牌失效。
     */
    @Test
    @Order(1)
    void fullAdminFlow_loginToMenuTreeToLogout() throws Exception {
        // 1. 获取验证码
        CaptchaResponse cap = captchaService.generate();
        String captchaCode = TestRedisConfiguration.getStore()
                .get("fep:captcha:" + cap.getCaptchaId());
        assertNotNull(captchaCode, "验证码应已存入模拟 Redis");

        // 2. 登录
        LoginRequest loginReq = new LoginRequest();
        loginReq.setAccount(TEST_ACCOUNT);
        loginReq.setPassword(TEST_PASSWORD);
        loginReq.setCaptchaId(cap.getCaptchaId());
        loginReq.setCaptchaCode(captchaCode);

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.roleCodes[0]").value("SYSTEM_ADMIN"))
                .andReturn();

        JsonNode body = objectMapper.readTree(
                loginResult.getResponse().getContentAsString());
        String accessToken = body.path("data").path("accessToken").asText();
        assertFalse(accessToken.isBlank(), "accessToken 不应为空");

        // 3. 访问受保护接口（用户列表）
        mockMvc.perform(get("/api/v1/sys/users")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.total").value(greaterThan(0)));

        // 4. 查询菜单树
        mockMvc.perform(get("/api/v1/sys/menus/my-tree")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data").isArray());

        // 5. 登出
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));

        // 6. 登出后 token 应失效 (黑名单)
        mockMvc.perform(get("/api/v1/sys/users")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());
    }

    /**
     * 单点踢出: 同账号第二次登录使第一次令牌失效 (PRD SS5.1.5)。
     */
    @Test
    @Order(2)
    void singleSignOn_secondLoginShouldKickOutFirst() throws Exception {
        // --- 第一次登录 ---
        String token1 = loginAndGetToken();

        // token1 可用
        mockMvc.perform(get("/api/v1/sys/users")
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk());

        // --- 第二次登录（同账号，新验证码） ---
        String token2 = loginAndGetToken();

        // token2 可用
        mockMvc.perform(get("/api/v1/sys/users")
                        .header("Authorization", "Bearer " + token2))
                .andExpect(status().isOk());

        // token1 应失效（被 token2 的 SSO 会话覆盖）
        mockMvc.perform(get("/api/v1/sys/users")
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isUnauthorized());
    }

    /**
     * 未认证请求应返回 401。
     */
    @Test
    @Order(3)
    void unauthenticatedAccessShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/sys/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_0401"));
    }

    /**
     * 验证码端点应公开可访问。
     */
    @Test
    @Order(4)
    void captchaEndpointShouldBePublic() throws Exception {
        mockMvc.perform(get("/api/v1/auth/captcha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.captchaId").isNotEmpty())
                .andExpect(jsonPath("$.data.imageBase64").isNotEmpty());
    }

    /**
     * Swagger UI 应公开可访问（非 401）。
     */
    @Test
    @Order(5)
    void swaggerUiShouldBePublic() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(result -> {
                    int httpStatus = result.getResponse().getStatus();
                    org.junit.jupiter.api.Assertions.assertNotEquals(
                            401, httpStatus, "Swagger UI 应为公开路径");
                });
    }

    // ========== Helper ==========

    /**
     * 执行登录流程并返回 accessToken。
     *
     * @return accessToken 字符串
     * @throws Exception MockMvc 异常
     */
    private String loginAndGetToken() throws Exception {
        CaptchaResponse cap = captchaService.generate();
        String code = TestRedisConfiguration.getStore()
                .get("fep:captcha:" + cap.getCaptchaId());

        LoginRequest req = new LoginRequest();
        req.setAccount(TEST_ACCOUNT);
        req.setPassword(TEST_PASSWORD);
        req.setCaptchaId(cap.getCaptchaId());
        req.setCaptchaCode(code);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(
                result.getResponse().getContentAsString());
        return body.path("data").path("accessToken").asText();
    }
}
