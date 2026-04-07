package com.puchain.fep.web.sysmgmt.config.pushinterface.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.puchain.fep.web.auth.domain.CaptchaResponse;
import com.puchain.fep.web.auth.domain.LoginRequest;
import com.puchain.fep.web.auth.service.CaptchaService;
import com.puchain.fep.web.config.TestRedisConfiguration;
import com.puchain.fep.web.sysmgmt.config.businesstype.domain.BusinessTypeStatus;
import com.puchain.fep.web.sysmgmt.config.businesstype.domain.SysBusinessType;
import com.puchain.fep.web.sysmgmt.config.businesstype.repository.SysBusinessTypeRepository;
import com.puchain.fep.web.sysmgmt.config.pushinterface.domain.PushMethod;
import com.puchain.fep.web.sysmgmt.config.pushinterface.dto.PushInterfaceCreateRequest;
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
 * 推送接口管理 Controller 集成测试。
 *
 * <p>使用全量 SpringBootTest + MockMvc，通过真实登录流程获取 JWT 令牌后访问受保护的
 * 推送接口管理接口。覆盖 CRUD、FK 外键校验及参数格式校验。</p>
 *
 * <p>参见 PRD v1.3 §5.10.7.2c 推送接口管理，FR-ID FR-WEB-SYS-CONF-PUSH。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@AutoConfigureMockMvc
class SysPushInterfaceControllerTest {

    /** 测试登录账号（满足 LoginRequest @Size(min=6) 约束）。 */
    private static final String TEST_ACCOUNT = "admins";

    /** 测试登录密码。 */
    private static final String TEST_PASSWORD = "admin@FEP2026";

    /** 种子数据中 admin 用户 ID。 */
    private static final String ADMIN_USER_ID = "00000000000000000000000000000001";

    /** 测试用业务类型 ID。 */
    private static final String TEST_BUSINESS_TYPE_ID = "test0000businesstype000000000001";

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

    /** 登录获取的 JWT 令牌，供测试方法使用。 */
    private String accessToken;

    /** 保存原始账号，测试结束后恢复。 */
    private String originalAccount;

    /** 保存原始密码哈希，测试结束后恢复。 */
    private String originalPasswordHash;

    /**
     * 每个测试前：设置测试账号、准备业务类型种子数据并登录获取 JWT 令牌。
     *
     * @throws Exception 登录请求异常
     */
    @BeforeEach
    void setUp() throws Exception {
        TestRedisConfiguration.getStore().clear();

        // 准备一个业务类型记录，供 FK 校验测试使用
        if (!businessTypeRepository.existsById(TEST_BUSINESS_TYPE_ID)) {
            SysBusinessType bt = new SysBusinessType();
            bt.setTypeId(TEST_BUSINESS_TYPE_ID);
            bt.setTypeName("测试业务类型");
            bt.setTypeCode("TEST_BT_001");
            bt.setSortOrder(99);
            bt.setTypeStatus(BusinessTypeStatus.ENABLED);
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
        assertFalse(accessToken.isBlank(), "accessToken 不应为空");
    }

    /**
     * 每个测试后：恢复 admin 账号，清空模拟 Redis，删除测试业务类型记录。
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

        businessTypeRepository.deleteById(TEST_BUSINESS_TYPE_ID);
        TestRedisConfiguration.getStore().clear();
    }

    /**
     * 创建有效推送接口应返回 200 并持久化数据。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void create_shouldPersist() throws Exception {
        PushInterfaceCreateRequest request = new PushInterfaceCreateRequest();
        request.setInterfaceName("测试推送接口");
        request.setInterfaceUrl("http://example.com/api/push");
        request.setPushMethod(PushMethod.AUTO);

        mockMvc.perform(post("/api/v1/sys/config/push-interfaces")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.interfaceId", notNullValue()))
                .andExpect(jsonPath("$.data.interfaceName", is("测试推送接口")))
                .andExpect(jsonPath("$.data.pushMethod", is("AUTO")))
                .andExpect(jsonPath("$.data.interfaceStatus", is("ENABLED")));
    }

    /**
     * 创建时接口名称超过 30 字符应返回 400 Bad Request。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void create_nameTooLong_shouldReturn400() throws Exception {
        PushInterfaceCreateRequest request = new PushInterfaceCreateRequest();
        // 31 字符名称（恰好超出 PRD 规定的 1-30 限制）
        request.setInterfaceName("这个接口名称超过三十个字符限制用于测试边界校验x0123456789");
        request.setInterfaceUrl("http://example.com/api/push");
        request.setPushMethod(PushMethod.AUTO);

        mockMvc.perform(post("/api/v1/sys/config/push-interfaces")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    /**
     * 创建时传入非法 URL 应返回 400 Bad Request。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void create_invalidUrl_shouldReturn400() throws Exception {
        PushInterfaceCreateRequest request = new PushInterfaceCreateRequest();
        request.setInterfaceName("非法URL接口");
        request.setInterfaceUrl("not-a-url");
        request.setPushMethod(PushMethod.MANUAL);

        mockMvc.perform(post("/api/v1/sys/config/push-interfaces")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    /**
     * 创建时关联不存在的业务类型 ID 应返回 400（BIZ_5004 映射到 400）。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void create_invalidBusinessType_shouldReturn400() throws Exception {
        PushInterfaceCreateRequest request = new PushInterfaceCreateRequest();
        request.setInterfaceName("FK校验接口");
        request.setInterfaceUrl("http://example.com/api/push");
        request.setPushMethod(PushMethod.AUTO);
        request.setBusinessTypeId("nonexistentbusinesstypeid000001");

        mockMvc.perform(post("/api/v1/sys/config/push-interfaces")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("BIZ_5004")));
    }

    /**
     * 搜索推送接口应返回分页结果。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void search_shouldReturnPagedResults() throws Exception {
        mockMvc.perform(get("/api/v1/sys/config/push-interfaces")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records").isArray());
    }

    /**
     * 更新推送接口应修改对应字段并返回更新后数据。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void update_shouldModify() throws Exception {
        // 先创建
        PushInterfaceCreateRequest createReq = new PushInterfaceCreateRequest();
        createReq.setInterfaceName("原接口名称");
        createReq.setInterfaceUrl("http://example.com/api/v1/push");
        createReq.setPushMethod(PushMethod.MANUAL);

        MvcResult createResult = mockMvc.perform(post("/api/v1/sys/config/push-interfaces")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isOk())
                .andReturn();

        String interfaceId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .at("/data/interfaceId").asText();

        // 更新
        PushInterfaceCreateRequest updateReq = new PushInterfaceCreateRequest();
        updateReq.setInterfaceName("新接口名称");
        updateReq.setInterfaceUrl("https://example.com/api/v2/push");
        updateReq.setPushMethod(PushMethod.AUTO);
        updateReq.setBusinessTypeId(TEST_BUSINESS_TYPE_ID);

        mockMvc.perform(put("/api/v1/sys/config/push-interfaces/" + interfaceId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.interfaceName", is("新接口名称")))
                .andExpect(jsonPath("$.data.pushMethod", is("AUTO")))
                .andExpect(jsonPath("$.data.businessTypeId", is(TEST_BUSINESS_TYPE_ID)))
                .andExpect(jsonPath("$.data.businessTypeName", is("测试业务类型")));
    }

    /**
     * 删除推送接口应成功移除记录。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void delete_shouldRemove() throws Exception {
        // 先创建
        PushInterfaceCreateRequest createReq = new PushInterfaceCreateRequest();
        createReq.setInterfaceName("待删除接口");
        createReq.setInterfaceUrl("http://example.com/api/delete-me");
        createReq.setPushMethod(PushMethod.AUTO);

        MvcResult createResult = mockMvc.perform(post("/api/v1/sys/config/push-interfaces")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isOk())
                .andReturn();

        String interfaceId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .at("/data/interfaceId").asText();

        // 删除
        mockMvc.perform(delete("/api/v1/sys/config/push-interfaces/" + interfaceId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        // 再次查询应返回 404
        mockMvc.perform(delete("/api/v1/sys/config/push-interfaces/" + interfaceId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("BIZ_5001")));
    }
}
