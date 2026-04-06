package com.puchain.fep.web.sysmgmt.message.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.puchain.fep.web.auth.domain.CaptchaResponse;
import com.puchain.fep.web.auth.domain.LoginRequest;
import com.puchain.fep.web.auth.service.CaptchaService;
import com.puchain.fep.web.config.TestRedisConfiguration;
import com.puchain.fep.web.sysmgmt.message.domain.MessageType;
import com.puchain.fep.web.sysmgmt.message.domain.ReceiverType;
import com.puchain.fep.web.sysmgmt.message.dto.MessageCreateRequest;
import com.puchain.fep.web.sysmgmt.message.repository.SysMessageReadRepository;
import com.puchain.fep.web.sysmgmt.message.repository.SysMessageRepository;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link SysMessageController} 集成测试。
 *
 * <p>使用全量 SpringBootTest + MockMvc，通过真实登录流程获取 JWT 令牌后访问受保护的
 * 消息管理接口。验证发布、管理员列表、未读计数、全部已读、删除、单条已读功能。
 * 参见 PRD v1.3 §5.10.4 消息管理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@AutoConfigureMockMvc
class SysMessageControllerTest {

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
    private SysMessageRepository messageRepository;

    @Autowired
    private SysMessageReadRepository messageReadRepository;

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
     * 每个测试前：清空模拟 Redis、将 admin 账号临时改为满足 min=6 约束的账号并完成登录以获取 JWT 令牌。
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
     * 每个测试后：清理消息相关数据，恢复 admin 账号，清空模拟 Redis。
     */
    @AfterEach
    void tearDown() {
        messageReadRepository.deleteAll();
        messageRepository.deleteAll();

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
     * 发布消息应返回 200 且响应体包含 messageId。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void publish_shouldCreateMessage() throws Exception {
        MessageCreateRequest req = buildBroadcastRequest();

        MvcResult result = mockMvc.perform(post("/api/v1/sys/messages")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.messageId").isNotEmpty())
                .andExpect(jsonPath("$.data.title").value("集成测试公告"))
                .andExpect(jsonPath("$.data.receiverType").value("ALL"))
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        assertNotNull(data.path("messageId").asText(), "messageId 不应为空");
    }

    /**
     * 管理员列表应包含已发布的消息（total >= 1）。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void adminList_shouldReturnMessages() throws Exception {
        // 先发布一条消息
        mockMvc.perform(post("/api/v1/sys/messages")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildBroadcastRequest())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/sys/messages")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.total").value(
                        org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.records[0].title").value("集成测试公告"));
    }

    /**
     * 未读计数接口应返回正确数量（发布1条广播消息后未读数 >= 1）。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void unreadCount_shouldReturnCount() throws Exception {
        // 发布一条广播消息
        mockMvc.perform(post("/api/v1/sys/messages")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildBroadcastRequest())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/sys/messages/mine/unread-count")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data").value(
                        org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    /**
     * 全部已读后未读计数应变为 0。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void readAll_shouldMarkAllRead() throws Exception {
        // 发布一条广播消息
        mockMvc.perform(post("/api/v1/sys/messages")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildBroadcastRequest())))
                .andExpect(status().isOk());

        // 执行全部已读
        mockMvc.perform(post("/api/v1/sys/messages/read-all")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));

        // 未读计数应变为 0
        mockMvc.perform(get("/api/v1/sys/messages/mine/unread-count")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(0));
    }

    /**
     * 删除消息后管理员列表中不应再包含该消息（total = 0）。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void delete_shouldLogicallyRemoveMessage() throws Exception {
        // 发布消息并获取 messageId
        MvcResult publishResult = mockMvc.perform(post("/api/v1/sys/messages")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildBroadcastRequest())))
                .andExpect(status().isOk())
                .andReturn();

        String messageId = objectMapper.readTree(
                publishResult.getResponse().getContentAsString())
                .path("data").path("messageId").asText();

        // 执行逻辑删除
        mockMvc.perform(delete("/api/v1/sys/messages/" + messageId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));

        // 管理员列表中不应包含已删除消息的记录（records 为空）
        mockMvc.perform(get("/api/v1/sys/messages")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records").isEmpty());
    }

    /**
     * 标记单条消息已读后，我的消息列表中该消息的 isRead 应为 true。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void markRead_shouldSucceed() throws Exception {
        // 发布消息并获取 messageId
        MvcResult publishResult = mockMvc.perform(post("/api/v1/sys/messages")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildBroadcastRequest())))
                .andExpect(status().isOk())
                .andReturn();

        String messageId = objectMapper.readTree(
                publishResult.getResponse().getContentAsString())
                .path("data").path("messageId").asText();

        // 标记已读
        mockMvc.perform(post("/api/v1/sys/messages/" + messageId + "/read")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));

        // 我的消息列表中该消息 isRead 应为 true
        mockMvc.perform(get("/api/v1/sys/messages/mine")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[?(@.messageId == '" + messageId + "')].read")
                        .value(true));
    }

    // ===== Helper =====

    /**
     * 构建广播类型的测试消息创建请求。
     *
     * @return 消息创建请求
     */
    private MessageCreateRequest buildBroadcastRequest() {
        MessageCreateRequest req = new MessageCreateRequest();
        req.setMessageType(MessageType.SYSTEM_NOTICE);
        req.setTitle("集成测试公告");
        req.setContent("这是集成测试发布的公告内容");
        req.setReceiverType(ReceiverType.ALL);
        return req;
    }
}
