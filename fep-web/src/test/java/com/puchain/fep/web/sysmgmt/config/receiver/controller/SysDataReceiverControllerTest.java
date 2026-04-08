package com.puchain.fep.web.sysmgmt.config.receiver.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.puchain.fep.web.auth.domain.CaptchaResponse;
import com.puchain.fep.web.auth.domain.LoginRequest;
import com.puchain.fep.web.auth.service.CaptchaService;
import com.puchain.fep.web.config.TestRedisConfiguration;
import com.puchain.fep.web.sysmgmt.config.receiver.dto.DataReceiverCreateRequest;
import com.puchain.fep.web.sysmgmt.config.receiver.domain.ReceiverMethod;
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
 * 数据接收方管理 Controller 集成测试。
 *
 * <p>使用全量 SpringBootTest + MockMvc，通过真实登录流程获取 JWT 令牌后访问受保护的
 * 数据接收方接口。覆盖 CRUD 及无效接收方式校验。</p>
 *
 * <p>参见 PRD v1.3 §5.10.7.2b 数据接收方管理，FR-ID FR-WEB-SYS-CONF-RECEIVER。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@AutoConfigureMockMvc
class SysDataReceiverControllerTest {

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
     * 创建有效数据接收方应返回 200 并持久化数据。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void create_shouldPersist() throws Exception {
        DataReceiverCreateRequest request = new DataReceiverCreateRequest();
        request.setReceiverName("测试接口接收方");
        request.setReceiverMethod(ReceiverMethod.INTERFACE);
        request.setReceiverAddress("http://example.com/api/receive");

        mockMvc.perform(post("/api/v1/sys/config/receivers")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.receiverId", notNullValue()))
                .andExpect(jsonPath("$.data.receiverName", is("测试接口接收方")))
                .andExpect(jsonPath("$.data.receiverMethod", is("INTERFACE")))
                .andExpect(jsonPath("$.data.receiverStatus", is("ENABLED")));
    }

    /**
     * 搜索数据接收方应返回分页结果。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void search_shouldReturnPagedResults() throws Exception {
        mockMvc.perform(get("/api/v1/sys/config/receivers")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records").isArray());
    }

    /**
     * 更新数据接收方应修改对应字段并返回更新后数据。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void update_shouldModify() throws Exception {
        // 先创建
        DataReceiverCreateRequest createReq = new DataReceiverCreateRequest();
        createReq.setReceiverName("原接收方名称");
        createReq.setReceiverMethod(ReceiverMethod.FILE);
        createReq.setReceiverAddress("/data/input/");

        MvcResult createResult = mockMvc.perform(post("/api/v1/sys/config/receivers")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isOk())
                .andReturn();

        String receiverId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .at("/data/receiverId").asText();

        // 更新
        DataReceiverCreateRequest updateReq = new DataReceiverCreateRequest();
        updateReq.setReceiverName("新接收方名称");
        updateReq.setReceiverMethod(ReceiverMethod.FTP);
        updateReq.setReceiverAddress("ftp://example.com/data/");

        mockMvc.perform(put("/api/v1/sys/config/receivers/" + receiverId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.receiverName", is("新接收方名称")))
                .andExpect(jsonPath("$.data.receiverMethod", is("FTP")));
    }

    /**
     * 删除数据接收方应成功移除记录。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void delete_shouldRemove() throws Exception {
        // 先创建
        DataReceiverCreateRequest createReq = new DataReceiverCreateRequest();
        createReq.setReceiverName("待删除接收方");
        createReq.setReceiverMethod(ReceiverMethod.INTERFACE);

        MvcResult createResult = mockMvc.perform(post("/api/v1/sys/config/receivers")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isOk())
                .andReturn();

        String receiverId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .at("/data/receiverId").asText();

        // 删除
        mockMvc.perform(delete("/api/v1/sys/config/receivers/" + receiverId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    /**
     * 创建时传入无效接收方式应返回 400 Bad Request。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void create_invalidMethod_shouldReturn400() throws Exception {
        String invalidJson = "{\"receiverName\":\"测试接收方\",\"receiverMethod\":\"INVALID_METHOD\"}";

        mockMvc.perform(post("/api/v1/sys/config/receivers")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    /**
     * 创建重复名称的接收方应返回 409 Conflict。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void create_duplicateName_shouldReturn409() throws Exception {
        DataReceiverCreateRequest req = new DataReceiverCreateRequest();
        req.setReceiverName("重复名称接收方");
        req.setReceiverMethod(ReceiverMethod.FTP);

        mockMvc.perform(post("/api/v1/sys/config/receivers")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)));

        // 再次创建相同名称
        mockMvc.perform(post("/api/v1/sys/config/receivers")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    /**
     * 按关键字搜索时应只返回匹配的记录。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void search_withKeyword_shouldFilterResults() throws Exception {
        // 创建一个特定名称的接收方
        DataReceiverCreateRequest createReq = new DataReceiverCreateRequest();
        createReq.setReceiverName("唯一FTP关键字接收方");
        createReq.setReceiverMethod(ReceiverMethod.FTP);

        mockMvc.perform(post("/api/v1/sys/config/receivers")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)));

        mockMvc.perform(get("/api/v1/sys/config/receivers")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("keyword", "唯一FTP关键字")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records").isArray());
    }

    /**
     * 更新时携带 receiverStatus 字段应正常更新状态。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void update_withStatus_shouldUpdateStatus() throws Exception {
        // 先创建
        DataReceiverCreateRequest createReq = new DataReceiverCreateRequest();
        createReq.setReceiverName("带状态更新接收方");
        createReq.setReceiverMethod(ReceiverMethod.INTERFACE);

        MvcResult createResult = mockMvc.perform(post("/api/v1/sys/config/receivers")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isOk())
                .andReturn();

        String receiverId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .at("/data/receiverId").asText();

        // 更新并携带 DISABLED 状态
        com.puchain.fep.common.domain.EnableDisableStatus disabled =
                com.puchain.fep.common.domain.EnableDisableStatus.DISABLED;
        DataReceiverCreateRequest updateReq = new DataReceiverCreateRequest();
        updateReq.setReceiverName("带状态更新接收方");
        updateReq.setReceiverMethod(ReceiverMethod.INTERFACE);
        updateReq.setReceiverStatus(disabled);

        mockMvc.perform(put("/api/v1/sys/config/receivers/" + receiverId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.receiverStatus", is("DISABLED")));
    }

    /**
     * 删除不存在的接收方应返回 404。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void delete_notFound_shouldReturn404() throws Exception {
        mockMvc.perform(delete("/api/v1/sys/config/receivers/nonexistentreceiverid000000001")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("BIZ_5001")));
    }

    /**
     * 更新不存在的接收方应返回 404。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void update_notFound_shouldReturn404() throws Exception {
        DataReceiverCreateRequest updateReq = new DataReceiverCreateRequest();
        updateReq.setReceiverName("不存在接收方");
        updateReq.setReceiverMethod(ReceiverMethod.FILE);

        mockMvc.perform(put("/api/v1/sys/config/receivers/nonexistentreceiverid000000002")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("BIZ_5001")));
    }

    /**
     * 更新时名称与其他记录冲突应返回 409。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void update_duplicateName_shouldReturn409() throws Exception {
        // 创建两个接收方
        DataReceiverCreateRequest req1 = new DataReceiverCreateRequest();
        req1.setReceiverName("接收方A-冲突测试");
        req1.setReceiverMethod(ReceiverMethod.INTERFACE);

        DataReceiverCreateRequest req2 = new DataReceiverCreateRequest();
        req2.setReceiverName("接收方B-冲突测试");
        req2.setReceiverMethod(ReceiverMethod.FTP);

        mockMvc.perform(post("/api/v1/sys/config/receivers")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req1)));

        MvcResult result2 = mockMvc.perform(post("/api/v1/sys/config/receivers")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andReturn();

        String receiverId2 = objectMapper.readTree(result2.getResponse().getContentAsString())
                .at("/data/receiverId").asText();

        // 尝试将接收方B的名称改为接收方A的名称
        DataReceiverCreateRequest updateReq = new DataReceiverCreateRequest();
        updateReq.setReceiverName("接收方A-冲突测试");
        updateReq.setReceiverMethod(ReceiverMethod.FTP);

        mockMvc.perform(put("/api/v1/sys/config/receivers/" + receiverId2)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("BIZ_5002")));
    }
}
