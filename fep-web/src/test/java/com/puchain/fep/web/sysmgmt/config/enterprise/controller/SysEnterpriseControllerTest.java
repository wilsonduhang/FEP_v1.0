package com.puchain.fep.web.sysmgmt.config.enterprise.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.puchain.fep.web.auth.domain.CaptchaResponse;
import com.puchain.fep.web.auth.domain.LoginRequest;
import com.puchain.fep.web.auth.service.CaptchaService;
import com.puchain.fep.web.config.TestRedisConfiguration;
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
 * 企业主体管理 Controller 集成测试。
 *
 * <p>使用全量 SpringBootTest + MockMvc，通过真实登录流程获取 JWT 令牌后访问受保护的
 * 企业主体接口。覆盖 CRUD 及 USCI 校验。</p>
 *
 * <p>参见 PRD v1.3 §5.10.7.3 企业主体管理，FR-ID FR-WEB-SYS-CONF-ENT。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@AutoConfigureMockMvc
class SysEnterpriseControllerTest {

    /** 测试登录账号（满足 LoginRequest @Size(min=6) 约束）。 */
    private static final String TEST_ACCOUNT = "admins";

    /** 测试登录密码。 */
    private static final String TEST_PASSWORD = "admin@FEP2026";

    /** 种子数据中 admin 用户 ID。 */
    private static final String ADMIN_USER_ID = "00000000000000000000000000000001";

    /** 企业主体基路径。 */
    private static final String BASE_URL = "/api/v1/sys/config/enterprises";

    /** 有效的 18 位 USCI（测试1）。 */
    private static final String USCI_1 = "91430100MA4L2YWK0T";

    /** 有效的 18 位 USCI（测试2 —— 短格式校验用）。 */
    private static final String USCI_SHORT = "123456";

    /** 有效的 18 位 USCI（测试3 —— 重复测试）。 */
    private static final String USCI_3 = "91430100MA4L2YWK1X";

    /** 有效的 18 位 USCI（测试5 —— 删除测试）。 */
    private static final String USCI_5 = "91430100MA4L2YWK2D";

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
     * 创建有效企业主体应返回 200，enterpriseId 不为空，auditStatus 为 PENDING。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void create_valid_shouldReturnPending() throws Exception {
        EnterpriseCreateRequest request = new EnterpriseCreateRequest();
        request.setEnterpriseName("湖南测试科技有限公司");
        request.setUsci(USCI_1);

        mockMvc.perform(post(BASE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enterpriseId", notNullValue()))
                .andExpect(jsonPath("$.data.auditStatus", is("PENDING")));
    }

    /**
     * 创建时 USCI 格式非法（6字符）应返回 400 Bad Request。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void create_invalidUsci_shouldReturn400() throws Exception {
        EnterpriseCreateRequest request = new EnterpriseCreateRequest();
        request.setEnterpriseName("格式错误企业");
        request.setUsci(USCI_SHORT);

        mockMvc.perform(post(BASE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    /**
     * 重复 USCI 应返回 409 Conflict。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void create_duplicateUsci_shouldReturn409() throws Exception {
        EnterpriseCreateRequest req = new EnterpriseCreateRequest();
        req.setEnterpriseName("首次创建企业");
        req.setUsci(USCI_3);

        mockMvc.perform(post(BASE_URL)
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)));

        EnterpriseCreateRequest req2 = new EnterpriseCreateRequest();
        req2.setEnterpriseName("重复USCI企业");
        req2.setUsci(USCI_3);

        mockMvc.perform(post(BASE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isConflict());
    }

    /**
     * 搜索企业主体应返回分页列表（data.list 为数组）。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void search_shouldReturnPagedList() throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records").isArray());
    }

    /**
     * 创建企业后删除应返回 200。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void createThenDelete_shouldReturn200() throws Exception {
        EnterpriseCreateRequest req = new EnterpriseCreateRequest();
        req.setEnterpriseName("待删除企业");
        req.setUsci(USCI_5);

        MvcResult createResult = mockMvc.perform(post(BASE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        String enterpriseId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .at("/data/enterpriseId").asText();

        mockMvc.perform(delete(BASE_URL + "/" + enterpriseId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    /**
     * 企业名称为空时应返回 400 Bad Request。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void create_blankName_shouldReturn400() throws Exception {
        EnterpriseCreateRequest request = new EnterpriseCreateRequest();
        request.setEnterpriseName("   ");
        request.setUsci("91430100MA4L2YWK3E");

        mockMvc.perform(post(BASE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    /**
     * 更新企业主体应修改对应字段并返回更新后数据（USCI 不变）。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void update_shouldModifyName() throws Exception {
        EnterpriseCreateRequest createReq = new EnterpriseCreateRequest();
        createReq.setEnterpriseName("原始企业名称");
        createReq.setUsci("91430100MA4L2YWK4F");

        MvcResult createResult = mockMvc.perform(post(BASE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isOk())
                .andReturn();

        String enterpriseId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .at("/data/enterpriseId").asText();

        EnterpriseCreateRequest updateReq = new EnterpriseCreateRequest();
        updateReq.setEnterpriseName("更新后企业名称");
        updateReq.setUsci("91430100MA4L2YWK4F");

        mockMvc.perform(put(BASE_URL + "/" + enterpriseId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enterpriseName", is("更新后企业名称")))
                .andExpect(jsonPath("$.data.usci", is("91430100MA4L2YWK4F")));
    }

    /**
     * 删除不存在的企业主体应返回 404。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void delete_notFound_shouldReturn404() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/nonexistententerpriseid00000001")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("BIZ_5001")));
    }

    /**
     * 按 ID 查询应返回正确的企业信息。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void getById_shouldReturnEnterprise() throws Exception {
        EnterpriseCreateRequest req = new EnterpriseCreateRequest();
        req.setEnterpriseName("按ID查询企业");
        req.setUsci("91430100MA4L2YWK5G");

        MvcResult createResult = mockMvc.perform(post(BASE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        String enterpriseId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .at("/data/enterpriseId").asText();

        mockMvc.perform(get(BASE_URL + "/" + enterpriseId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enterpriseName", is("按ID查询企业")))
                .andExpect(jsonPath("$.data.usci", is("91430100MA4L2YWK5G")));
    }

    /**
     * 按 ID 查询不存在的企业主体应返回 404。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void getById_notFound_shouldReturn404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/nonexistententerpriseid00000002")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("BIZ_5001")));
    }

    /**
     * 更新不存在的企业主体应返回 404。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void update_notFound_shouldReturn404() throws Exception {
        EnterpriseCreateRequest updateReq = new EnterpriseCreateRequest();
        updateReq.setEnterpriseName("不存在企业");
        updateReq.setUsci("91430100MA4L2YWK6H");

        mockMvc.perform(put(BASE_URL + "/nonexistententerpriseid00000003")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("BIZ_5001")));
    }

    /**
     * 按审核状态过滤搜索应返回过滤结果。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void search_withAuditStatus_shouldReturnFiltered() throws Exception {
        EnterpriseCreateRequest req = new EnterpriseCreateRequest();
        req.setEnterpriseName("状态过滤测试企业");
        req.setUsci("91430100MA4L2YWK7J");

        mockMvc.perform(post(BASE_URL)
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)));

        mockMvc.perform(get(BASE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .param("auditStatus", "PENDING")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records").isArray());
    }

    /**
     * 按关键字搜索应返回匹配结果。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void search_withKeyword_shouldReturnFiltered() throws Exception {
        EnterpriseCreateRequest req = new EnterpriseCreateRequest();
        req.setEnterpriseName("关键字过滤唯一测试企业XYZ");
        req.setUsci("91430100MA4L2YWK8K");

        mockMvc.perform(post(BASE_URL)
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)));

        mockMvc.perform(get(BASE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .param("keyword", "关键字过滤唯一")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records").isArray());
    }
}
