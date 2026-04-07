package com.puchain.fep.web.sysmgmt.config.alert.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.puchain.fep.web.auth.domain.CaptchaResponse;
import com.puchain.fep.web.auth.domain.LoginRequest;
import com.puchain.fep.web.auth.service.CaptchaService;
import com.puchain.fep.web.config.TestRedisConfiguration;
import com.puchain.fep.web.sysmgmt.config.alert.domain.AlertFrequency;
import com.puchain.fep.web.sysmgmt.config.alert.domain.NotifyMethod;
import com.puchain.fep.web.sysmgmt.config.alert.dto.AlertRuleUpdateRequest;
import com.puchain.fep.web.sysmgmt.config.alert.domain.SysAlertRule;
import com.puchain.fep.web.sysmgmt.config.alert.repository.SysAlertRuleRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 接口预警管理 Controller 集成测试。
 *
 * <p>使用全量 SpringBootTest + MockMvc，通过真实登录流程获取 JWT 令牌后访问受保护的
 * 接口预警管理接口。覆盖种子数据读取、更新及参数格式校验。</p>
 *
 * <p>参见 PRD v1.3 §5.10.7.2d 接口预警管理，FR-ID FR-WEB-SYS-CONF-ALERT。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@AutoConfigureMockMvc
class SysAlertRuleControllerTest {

    /** 测试登录账号（满足 LoginRequest @Size(min=6) 约束）。 */
    private static final String TEST_ACCOUNT = "admins";

    /** 测试登录密码。 */
    private static final String TEST_PASSWORD = "admin@FEP2026";

    /** 种子数据中 admin 用户 ID。 */
    private static final String ADMIN_USER_ID = "00000000000000000000000000000001";

    /** V6 种子数据中默认预警规则 ID。 */
    private static final String DEFAULT_RULE_ID = "default_alert_rule_00000000001";

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
    private SysAlertRuleRepository alertRuleRepository;

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
        assertFalse(accessToken.isBlank(), "accessToken 不应为空");
    }

    /**
     * 每个测试后：恢复 admin 账号、恢复预警规则至种子数据初始值，清空模拟 Redis。
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

        // 恢复预警规则至 V6 种子数据初始状态，避免测试间状态污染
        alertRuleRepository.findById(DEFAULT_RULE_ID).ifPresent(rule -> {
            rule.setAlertEnabled(false);
            rule.setThreshold(0);
            rule.setAlertEmail(null);
            rule.setNotifyMethod(com.puchain.fep.web.sysmgmt.config.alert.domain.NotifyMethod.EMAIL);
            rule.setAlertFrequency(com.puchain.fep.web.sysmgmt.config.alert.domain.AlertFrequency.REALTIME);
            alertRuleRepository.save(rule);
        });

        TestRedisConfiguration.getStore().clear();
    }

    /**
     * 查询预警规则应返回 V6 种子数据（rule_id=default_alert_rule_00000000001,
     * alertEnabled=false, threshold=0, notifyMethod=EMAIL, alertFrequency=REALTIME）。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void getRule_shouldReturnDefaultConfig() throws Exception {
        mockMvc.perform(get("/api/v1/sys/config/alert-rules")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.ruleId").value(DEFAULT_RULE_ID))
                .andExpect(jsonPath("$.data.alertEnabled").value(false))
                .andExpect(jsonPath("$.data.threshold").value(0))
                .andExpect(jsonPath("$.data.notifyMethod").value("EMAIL"))
                .andExpect(jsonPath("$.data.alertFrequency").value("REALTIME"));
    }

    /**
     * 更新预警规则：启用预警、设置阈值=100、设置邮箱、更改通知方式，验证持久化结果。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void updateRule_shouldModifyConfig() throws Exception {
        AlertRuleUpdateRequest request = new AlertRuleUpdateRequest();
        request.setAlertEnabled(true);
        request.setThreshold(100);
        request.setAlertEmail("alert@example.com");
        request.setNotifyMethod(NotifyMethod.SMS);
        request.setAlertFrequency(AlertFrequency.HOURLY);

        mockMvc.perform(put("/api/v1/sys/config/alert-rules")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.alertEnabled").value(true))
                .andExpect(jsonPath("$.data.threshold").value(100))
                .andExpect(jsonPath("$.data.alertEmail").value("alert@example.com"))
                .andExpect(jsonPath("$.data.notifyMethod").value("SMS"))
                .andExpect(jsonPath("$.data.alertFrequency").value("HOURLY"));

        // 验证更新后 GET 返回一致
        mockMvc.perform(get("/api/v1/sys/config/alert-rules")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.alertEnabled").value(true))
                .andExpect(jsonPath("$.data.threshold").value(100));
    }

    /**
     * 提交非法邮箱格式（"not-an-email"）应返回 HTTP 400。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void updateRule_invalidEmail_shouldReturn400() throws Exception {
        AlertRuleUpdateRequest request = new AlertRuleUpdateRequest();
        request.setAlertEnabled(false);
        request.setThreshold(0);
        request.setAlertEmail("not-an-email");
        request.setNotifyMethod(NotifyMethod.EMAIL);
        request.setAlertFrequency(AlertFrequency.REALTIME);

        mockMvc.perform(put("/api/v1/sys/config/alert-rules")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    /**
     * 提交负数阈值（threshold=-1）应返回 HTTP 400。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void updateRule_negativeThreshold_shouldReturn400() throws Exception {
        AlertRuleUpdateRequest request = new AlertRuleUpdateRequest();
        request.setAlertEnabled(false);
        request.setThreshold(-1);
        request.setAlertEmail(null);
        request.setNotifyMethod(NotifyMethod.EMAIL);
        request.setAlertFrequency(AlertFrequency.REALTIME);

        mockMvc.perform(put("/api/v1/sys/config/alert-rules")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    /**
     * 若表中无预警规则配置，GET 应返回 HTTP 404（BIZ_5001）。
     *
     * <p>测试前删除种子行，测试后由 tearDown 重新插入（通过 alertRuleRepository.save()）。</p>
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void getRule_noData_shouldReturn404() throws Exception {
        // 暂时删除种子行，触发 BIZ_5001 分支
        SysAlertRule seed = alertRuleRepository.findById(DEFAULT_RULE_ID).orElseThrow();
        alertRuleRepository.delete(seed);

        mockMvc.perform(get("/api/v1/sys/config/alert-rules")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());

        // 恢复种子行供 tearDown 使用
        seed.setAlertEnabled(false);
        seed.setThreshold(0);
        seed.setAlertEmail(null);
        seed.setNotifyMethod(NotifyMethod.EMAIL);
        seed.setAlertFrequency(AlertFrequency.REALTIME);
        alertRuleRepository.save(seed);
    }

    /**
     * 若表中无预警规则配置，PUT 应返回 HTTP 404（BIZ_5001）。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void updateRule_noData_shouldReturn404() throws Exception {
        // 暂时删除种子行，触发 BIZ_5001 分支
        SysAlertRule seed = alertRuleRepository.findById(DEFAULT_RULE_ID).orElseThrow();
        alertRuleRepository.delete(seed);

        AlertRuleUpdateRequest request = new AlertRuleUpdateRequest();
        request.setAlertEnabled(false);
        request.setThreshold(0);
        request.setAlertEmail(null);
        request.setNotifyMethod(NotifyMethod.EMAIL);
        request.setAlertFrequency(AlertFrequency.REALTIME);

        mockMvc.perform(put("/api/v1/sys/config/alert-rules")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());

        // 恢复种子行供 tearDown 使用
        seed.setAlertEnabled(false);
        seed.setThreshold(0);
        seed.setAlertEmail(null);
        seed.setNotifyMethod(NotifyMethod.EMAIL);
        seed.setAlertFrequency(AlertFrequency.REALTIME);
        alertRuleRepository.save(seed);
    }
}
