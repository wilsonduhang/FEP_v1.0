package com.puchain.fep.web.sysmgmt.config.platform.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.puchain.fep.web.auth.domain.CaptchaResponse;
import com.puchain.fep.web.auth.domain.LoginRequest;
import com.puchain.fep.web.auth.service.CaptchaService;
import com.puchain.fep.web.config.TestRedisConfiguration;
import com.puchain.fep.web.sysmgmt.config.platform.dto.ConfigBatchUpdateRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link SysConfigController} 集成测试。
 *
 * <p>使用全量 SpringBootTest + MockMvc，通过真实登录流程获取 JWT 令牌后访问受保护的
 * 系统配置接口。验证 PLATFORM / SYSTEM / CERT 分组读取、批量更新及格式校验功能。</p>
 *
 * <p>参见 PRD v1.3 §5.10.7.1 平台基础设置 + §5.10.7.4 其他系统配置。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@AutoConfigureMockMvc
class SysConfigControllerTest {

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
     * 每个测试前：设置测试账号并登录获取 JWT 令牌。
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
     * 查询 PLATFORM 分组应返回种子数据（至少 6 条）。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void getPlatformConfig_shouldReturnSeededValues() throws Exception {
        mockMvc.perform(get("/api/v1/sys/config/PLATFORM")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.configGroup").value("PLATFORM"))
                .andExpect(jsonPath("$.data.configs", hasSize(greaterThanOrEqualTo(6))));
    }

    /**
     * 查询 SYSTEM 分组应返回种子数据（至少 7 条）。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void getSystemConfig_shouldReturnSeededValues() throws Exception {
        mockMvc.perform(get("/api/v1/sys/config/SYSTEM")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.configGroup").value("SYSTEM"))
                .andExpect(jsonPath("$.data.configs", hasSize(greaterThanOrEqualTo(7))));
    }

    /**
     * 查询 CERT 分组应返回种子数据（至少 7 条）。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void getCertConfig_shouldReturnSeededValues() throws Exception {
        mockMvc.perform(get("/api/v1/sys/config/CERT")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.configGroup").value("CERT"))
                .andExpect(jsonPath("$.data.configs", hasSize(greaterThanOrEqualTo(7))));
    }

    /**
     * 查询不存在的分组应返回空列表。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void getConfigGroup_nonExistent_shouldReturnEmptyList() throws Exception {
        mockMvc.perform(get("/api/v1/sys/config/NON_EXISTENT")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.configs", hasSize(0)));
    }

    /**
     * 批量更新 PLATFORM 分组配置，验证值被持久化。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void batchUpdatePlatformConfig_shouldUpdateValues() throws Exception {
        ConfigBatchUpdateRequest request = new ConfigBatchUpdateRequest();
        request.setConfigs(Map.of("PLATFORM_NAME", "New Platform Name"));

        mockMvc.perform(put("/api/v1/sys/config/PLATFORM")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));

        // 验证更新后的值
        mockMvc.perform(get("/api/v1/sys/config/PLATFORM")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.data.configs[?(@.configKey=='PLATFORM_NAME')].configValue")
                        .value("New Platform Name"));
    }

    /**
     * SERVICE_PHONE 传入非法格式（含字母）应返回 HTTP 400。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void batchUpdateInvalidPhone_shouldReturn400() throws Exception {
        ConfigBatchUpdateRequest request = new ConfigBatchUpdateRequest();
        request.setConfigs(Map.of("SERVICE_PHONE", "not-a-phone-number"));

        mockMvc.perform(put("/api/v1/sys/config/PLATFORM")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    /**
     * PLATFORM_NAME 超过 30 字符应返回 HTTP 400。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void batchUpdatePlatformNameTooLong_shouldReturn400() throws Exception {
        ConfigBatchUpdateRequest request = new ConfigBatchUpdateRequest();
        request.setConfigs(Map.of("PLATFORM_NAME", "A".repeat(31)));

        mockMvc.perform(put("/api/v1/sys/config/PLATFORM")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
