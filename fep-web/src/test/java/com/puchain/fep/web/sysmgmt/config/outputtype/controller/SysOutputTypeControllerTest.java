package com.puchain.fep.web.sysmgmt.config.outputtype.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.puchain.fep.web.auth.domain.CaptchaResponse;
import com.puchain.fep.web.auth.domain.LoginRequest;
import com.puchain.fep.web.auth.service.CaptchaService;
import com.puchain.fep.web.config.TestRedisConfiguration;
import com.puchain.fep.web.sysmgmt.config.outputtype.dto.OutputTypeCreateRequest;
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
 * 输出类型管理 Controller 集成测试。
 *
 * <p>使用全量 SpringBootTest + MockMvc，通过真实登录流程获取 JWT 令牌后访问受保护的
 * 输出类型接口。覆盖 CRUD 和重复编码校验。</p>
 *
 * <p>参见 PRD v1.3 §5.10.7.2e 输出类型管理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@AutoConfigureMockMvc
class SysOutputTypeControllerTest {

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
     * 创建有效输出类型应返回 200 并持久化数据。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void create_shouldPersist() throws Exception {
        OutputTypeCreateRequest request = new OutputTypeCreateRequest();
        request.setTypeName("Excel 输出");
        request.setTypeCode("OUT_EXCEL_TEST");

        mockMvc.perform(post("/api/v1/sys/config/output-types")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.outputTypeId", notNullValue()))
                .andExpect(jsonPath("$.data.typeName", is("Excel 输出")))
                .andExpect(jsonPath("$.data.typeCode", is("OUT_EXCEL_TEST")))
                .andExpect(jsonPath("$.data.typeStatus", is("ENABLED")));
    }

    /**
     * 创建重复编码的输出类型应返回 409 Conflict。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void create_duplicateCode_shouldReturn409() throws Exception {
        OutputTypeCreateRequest req = new OutputTypeCreateRequest();
        req.setTypeName("PDF 输出");
        req.setTypeCode("OUT_PDF_DUP_TEST");

        mockMvc.perform(post("/api/v1/sys/config/output-types")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)));

        // 重复创建相同编码
        mockMvc.perform(post("/api/v1/sys/config/output-types")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    /**
     * 搜索输出类型应返回分页结果。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void search_shouldReturnPagedResults() throws Exception {
        mockMvc.perform(get("/api/v1/sys/config/output-types")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records").isArray());
    }

    /**
     * 更新输出类型应修改对应字段并返回更新后数据。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void update_shouldModify() throws Exception {
        // 先创建
        OutputTypeCreateRequest createReq = new OutputTypeCreateRequest();
        createReq.setTypeName("原输出类型");
        createReq.setTypeCode("OUT_UPDATE_TEST");

        MvcResult createResult = mockMvc.perform(post("/api/v1/sys/config/output-types")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isOk())
                .andReturn();

        String outputTypeId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .at("/data/outputTypeId").asText();

        // 更新
        OutputTypeCreateRequest updateReq = new OutputTypeCreateRequest();
        updateReq.setTypeName("新输出类型");
        updateReq.setTypeCode("OUT_UPDATE_TEST");

        mockMvc.perform(put("/api/v1/sys/config/output-types/" + outputTypeId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.typeName", is("新输出类型")));
    }

    /**
     * 删除输出类型应成功移除记录。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void delete_shouldRemove() throws Exception {
        // 先创建
        OutputTypeCreateRequest createReq = new OutputTypeCreateRequest();
        createReq.setTypeName("待删除输出类型");
        createReq.setTypeCode("OUT_DELETE_TEST");

        MvcResult createResult = mockMvc.perform(post("/api/v1/sys/config/output-types")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isOk())
                .andReturn();

        String outputTypeId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .at("/data/outputTypeId").asText();

        // 删除
        mockMvc.perform(delete("/api/v1/sys/config/output-types/" + outputTypeId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }
}
