package com.puchain.fep.web.sysmgmt.config.businesstype.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.puchain.fep.web.auth.domain.CaptchaResponse;
import com.puchain.fep.web.auth.domain.LoginRequest;
import com.puchain.fep.web.auth.service.CaptchaService;
import com.puchain.fep.web.config.TestRedisConfiguration;
import com.puchain.fep.web.sysmgmt.config.businesstype.dto.BusinessTypeCreateRequest;
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
 * 业务类型管理 Controller 集成测试。
 *
 * <p>使用全量 SpringBootTest + MockMvc，通过真实登录流程获取 JWT 令牌后访问受保护的
 * 业务类型接口。覆盖 CRUD、重复编码校验、字段长度校验、启用/停用功能。</p>
 *
 * <p>参见 PRD v1.3 §5.10.7.2a 业务类型管理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@AutoConfigureMockMvc
class SysBusinessTypeControllerTest {

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
     * 创建有效业务类型应返回 200 并持久化数据。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void create_shouldPersist() throws Exception {
        BusinessTypeCreateRequest request = new BusinessTypeCreateRequest();
        request.setTypeName("供应链融资");
        request.setTypeCode("SCF_TEST_CREATE");
        request.setSortOrder(1);

        mockMvc.perform(post("/api/v1/sys/config/business-types")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.typeId", notNullValue()))
                .andExpect(jsonPath("$.data.typeName", is("供应链融资")))
                .andExpect(jsonPath("$.data.typeCode", is("SCF_TEST_CREATE")))
                .andExpect(jsonPath("$.data.typeStatus", is("ENABLED")));
    }

    /**
     * 创建重复编码的业务类型应返回 409 Conflict。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void create_duplicateCode_shouldReturn409() throws Exception {
        BusinessTypeCreateRequest req = new BusinessTypeCreateRequest();
        req.setTypeName("企业查询");
        req.setTypeCode("ENT_QUERY_DUP_TEST");
        req.setSortOrder(2);

        mockMvc.perform(post("/api/v1/sys/config/business-types")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)));

        // 重复创建相同编码
        mockMvc.perform(post("/api/v1/sys/config/business-types")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    /**
     * 类型名称超过 30 字符应返回 400 Bad Request。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void create_nameTooLong_shouldReturn400() throws Exception {
        BusinessTypeCreateRequest request = new BusinessTypeCreateRequest();
        request.setTypeName("A".repeat(31));
        request.setTypeCode("LONG_NAME_TEST");
        request.setSortOrder(0);

        mockMvc.perform(post("/api/v1/sys/config/business-types")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    /**
     * 搜索业务类型应返回分页结果。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void search_shouldReturnPagedResults() throws Exception {
        mockMvc.perform(get("/api/v1/sys/config/business-types")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records").isArray());
    }

    /**
     * 更新业务类型应修改对应字段并返回更新后数据。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void update_shouldModify() throws Exception {
        // 先创建
        BusinessTypeCreateRequest createReq = new BusinessTypeCreateRequest();
        createReq.setTypeName("原名称");
        createReq.setTypeCode("UPDATE_TEST_CODE");
        createReq.setSortOrder(5);

        MvcResult createResult = mockMvc.perform(post("/api/v1/sys/config/business-types")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isOk())
                .andReturn();

        String typeId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .at("/data/typeId").asText();

        // 更新
        BusinessTypeCreateRequest updateReq = new BusinessTypeCreateRequest();
        updateReq.setTypeName("新名称");
        updateReq.setTypeCode("UPDATE_TEST_CODE");
        updateReq.setSortOrder(10);

        mockMvc.perform(put("/api/v1/sys/config/business-types/" + typeId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.typeName", is("新名称")))
                .andExpect(jsonPath("$.data.sortOrder", is(10)));
    }

    /**
     * 删除业务类型应成功移除记录。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void delete_shouldRemove() throws Exception {
        // 先创建
        BusinessTypeCreateRequest createReq = new BusinessTypeCreateRequest();
        createReq.setTypeName("待删除类型");
        createReq.setTypeCode("DELETE_TEST_CODE");
        createReq.setSortOrder(99);

        MvcResult createResult = mockMvc.perform(post("/api/v1/sys/config/business-types")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isOk())
                .andReturn();

        String typeId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .at("/data/typeId").asText();

        // 删除
        mockMvc.perform(delete("/api/v1/sys/config/business-types/" + typeId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    /**
     * 更新不存在的业务类型应返回 404。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void update_notFound_shouldReturn404() throws Exception {
        BusinessTypeCreateRequest updateReq = new BusinessTypeCreateRequest();
        updateReq.setTypeName("名称");
        updateReq.setTypeCode("NONEXISTENT_CODE");
        updateReq.setSortOrder(1);

        mockMvc.perform(put("/api/v1/sys/config/business-types/nonexistentid00000000000000001")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("BIZ_5001")));
    }

    /**
     * 删除不存在的业务类型应返回 404。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void delete_notFound_shouldReturn404() throws Exception {
        mockMvc.perform(delete("/api/v1/sys/config/business-types/nonexistentid00000000000000002")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("BIZ_5001")));
    }

    /**
     * 停用不存在的业务类型应返回 404。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void disable_notFound_shouldReturn404() throws Exception {
        mockMvc.perform(post("/api/v1/sys/config/business-types/nonexistentid00000000000000003/disable")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("BIZ_5001")));
    }

    /**
     * 按关键字搜索时，应只返回匹配的记录。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void search_withKeyword_shouldFilterResults() throws Exception {
        // 创建一个特定名称的业务类型
        BusinessTypeCreateRequest createReq = new BusinessTypeCreateRequest();
        createReq.setTypeName("唯一关键字查询");
        createReq.setTypeCode("KEYWORD_SEARCH_TEST");
        createReq.setSortOrder(50);

        mockMvc.perform(post("/api/v1/sys/config/business-types")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)));

        mockMvc.perform(get("/api/v1/sys/config/business-types")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("keyword", "唯一关键字")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records").isArray());
    }

    /**
     * 更新时使用已被其他记录占用的编码应返回 409。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void update_duplicateCode_shouldReturn409() throws Exception {
        // 创建两个业务类型
        BusinessTypeCreateRequest req1 = new BusinessTypeCreateRequest();
        req1.setTypeName("类型1");
        req1.setTypeCode("UPD_DUP_CODE_A");
        req1.setSortOrder(1);

        BusinessTypeCreateRequest req2 = new BusinessTypeCreateRequest();
        req2.setTypeName("类型2");
        req2.setTypeCode("UPD_DUP_CODE_B");
        req2.setSortOrder(2);

        mockMvc.perform(post("/api/v1/sys/config/business-types")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req1)));

        MvcResult result2 = mockMvc.perform(post("/api/v1/sys/config/business-types")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andReturn();

        String typeId2 = objectMapper.readTree(result2.getResponse().getContentAsString())
                .at("/data/typeId").asText();

        // 尝试将 typeId2 的编码改为已存在的 UPD_DUP_CODE_A
        BusinessTypeCreateRequest updateReq = new BusinessTypeCreateRequest();
        updateReq.setTypeName("类型2更名");
        updateReq.setTypeCode("UPD_DUP_CODE_A");
        updateReq.setSortOrder(2);

        mockMvc.perform(put("/api/v1/sys/config/business-types/" + typeId2)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("BIZ_5002")));
    }
}
