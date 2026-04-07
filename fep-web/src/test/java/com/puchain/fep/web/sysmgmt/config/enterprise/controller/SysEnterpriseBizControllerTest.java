package com.puchain.fep.web.sysmgmt.config.enterprise.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.puchain.fep.web.auth.domain.CaptchaResponse;
import com.puchain.fep.web.auth.domain.LoginRequest;
import com.puchain.fep.web.auth.service.CaptchaService;
import com.puchain.fep.web.config.TestRedisConfiguration;
import com.puchain.fep.web.sysmgmt.config.businesstype.dto.BusinessTypeCreateRequest;
import com.puchain.fep.web.sysmgmt.config.enterprise.dto.EnterpriseBizInfoRequest;
import com.puchain.fep.web.sysmgmt.config.enterprise.dto.EnterpriseCreateRequest;
import com.puchain.fep.web.sysmgmt.config.enterprise.dto.EnterpriseQueryConfigRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 企业业务信息关联 + 精准查询配置 Controller 集成测试。
 *
 * <p>覆盖 Tab2（业务信息）和 Tab3（精准查询配置）的核心接口。
 * 参见 PRD v1.3 §5.10.7.3 企业主体管理，FR-ID FR-WEB-SYS-CONF-ENT。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@AutoConfigureMockMvc
class SysEnterpriseBizControllerTest {

    /** 测试登录账号（满足 LoginRequest @Size(min=6) 约束）。 */
    private static final String TEST_ACCOUNT = "admins";

    /** 测试登录密码。 */
    private static final String TEST_PASSWORD = "admin@FEP2026";

    /** 种子数据中 admin 用户 ID。 */
    private static final String ADMIN_USER_ID = "00000000000000000000000000000001";

    /** 企业主体基路径。 */
    private static final String BASE_URL = "/api/v1/sys/config/enterprises";

    /** 业务类型基路径。 */
    private static final String BT_BASE_URL = "/api/v1/sys/config/business-types";

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

    /** 测试用企业 ID。 */
    private String enterpriseId;

    /** 测试用业务类型 ID。 */
    private String businessTypeId;

    /**
     * 每个测试前：登录获取 JWT 令牌，创建测试企业和业务类型。
     *
     * @throws Exception MockMvc 请求异常
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

        JsonNode loginBody = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        accessToken = loginBody.path("data").path("accessToken").asText();
        assertFalse(accessToken.isBlank(), "accessToken 不应为空");

        // Create enterprise with unique 18-char USCI
        long nano = System.nanoTime();
        String usci = "9143" + String.format("%014d", Math.abs(nano) % 100000000000000L);

        EnterpriseCreateRequest entReq = new EnterpriseCreateRequest();
        entReq.setEnterpriseName("业务关联测试企业");
        entReq.setUsci(usci);

        MvcResult entResult = mockMvc.perform(post(BASE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(entReq)))
                .andExpect(status().isOk())
                .andReturn();
        enterpriseId = objectMapper.readTree(entResult.getResponse().getContentAsString())
                .at("/data/enterpriseId").asText();

        // Create business type with unique code
        BusinessTypeCreateRequest btReq = new BusinessTypeCreateRequest();
        btReq.setTypeName("测试业务类型");
        btReq.setTypeCode("BIZ_TEST_" + Math.abs(nano) % 1000000L);
        btReq.setSortOrder(0);

        MvcResult btResult = mockMvc.perform(post(BT_BASE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(btReq)))
                .andExpect(status().isOk())
                .andReturn();
        businessTypeId = objectMapper.readTree(btResult.getResponse().getContentAsString())
                .at("/data/typeId").asText();
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
     * 添加业务信息关联应返回 200，data.id 不为空。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void addBizInfo_shouldReturn200() throws Exception {
        EnterpriseBizInfoRequest request = new EnterpriseBizInfoRequest();
        request.setBusinessTypeId(businessTypeId);

        mockMvc.perform(post(BASE_URL + "/" + enterpriseId + "/business-info")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", notNullValue()));
    }

    /**
     * 查询企业业务信息列表应返回 200，data 为数组。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void listBizInfo_shouldReturnList() throws Exception {
        // Add one first
        EnterpriseBizInfoRequest request = new EnterpriseBizInfoRequest();
        request.setBusinessTypeId(businessTypeId);
        mockMvc.perform(post(BASE_URL + "/" + enterpriseId + "/business-info")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(get(BASE_URL + "/" + enterpriseId + "/business-info")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    /**
     * 添加后删除业务信息关联应返回 200。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void deleteBizInfo_shouldRemove() throws Exception {
        EnterpriseBizInfoRequest request = new EnterpriseBizInfoRequest();
        request.setBusinessTypeId(businessTypeId);

        MvcResult addResult = mockMvc.perform(post(BASE_URL + "/" + enterpriseId + "/business-info")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String bizInfoId = objectMapper.readTree(addResult.getResponse().getContentAsString())
                .at("/data/id").asText();

        mockMvc.perform(delete(BASE_URL + "/" + enterpriseId + "/business-info/" + bizInfoId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    /**
     * 查询精准查询配置（未配置）应返回 200。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void getQueryConfig_shouldReturn200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/" + enterpriseId + "/query-config")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    /**
     * 更新精准查询配置应持久化 queryType 并返回 200。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void updateQueryConfig_shouldPersist() throws Exception {
        EnterpriseQueryConfigRequest request = new EnterpriseQueryConfigRequest();
        request.setQueryType("PRECISE");

        mockMvc.perform(put(BASE_URL + "/" + enterpriseId + "/query-config")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.queryType").value("PRECISE"));
    }
}
