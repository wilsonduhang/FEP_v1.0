package com.puchain.fep.web.sysmgmt.help.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.puchain.fep.web.auth.domain.CaptchaResponse;
import com.puchain.fep.web.auth.domain.LoginRequest;
import com.puchain.fep.web.auth.service.CaptchaService;
import com.puchain.fep.web.config.TestRedisConfiguration;
import com.puchain.fep.web.sysmgmt.help.domain.SysHelpContent;
import com.puchain.fep.web.sysmgmt.help.dto.HelpCreateRequest;
import com.puchain.fep.web.sysmgmt.help.repository.SysHelpContentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link SysHelpContentController} 集成测试。
 *
 * <p>使用全量 SpringBootTest + MockMvc，通过真实登录流程获取 JWT 令牌后访问受保护的
 * 帮助面板接口。验证按页面编码查询、无数据返回空列表、新增帮助内容功能。
 * 参见 PRD v1.3 §5.10.8 上下文帮助面板（FR-WEB-SYS-HELP）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@AutoConfigureMockMvc
class SysHelpContentControllerTest {

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
    private SysHelpContentRepository helpRepository;

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
     * 每个测试后：清理帮助内容测试数据，恢复 admin 账号，清空模拟 Redis。
     */
    @AfterEach
    void tearDown() {
        // 仅删除测试中插入的非种子数据（避免影响种子帮助数据）
        helpRepository.findAll().stream()
                .filter(h -> h.getPageCode().startsWith("test-"))
                .forEach(h -> helpRepository.deleteById(h.getHelpId()));

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
     * 按页面编码查询应返回 ACTIVE 状态的帮助内容。
     *
     * <p>插入两条 test-page 帮助条目，查询后应返回 2 条，标题与插入值一致。</p>
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void findByPageCode_shouldReturnActiveHelp() throws Exception {
        // 插入测试数据
        insertHelp("test-h-001", "test-page", "测试标题一", "摘要一", "内容一", 1, "ACTIVE");
        insertHelp("test-h-002", "test-page", "测试标题二", "摘要二", "内容二", 2, "ACTIVE");

        mockMvc.perform(get("/api/v1/sys/help")
                        .param("pageCode", "test-page")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].title").value("测试标题一"))
                .andExpect(jsonPath("$.data[0].pageCode").value("test-page"))
                .andExpect(jsonPath("$.data[1].title").value("测试标题二"));
    }

    /**
     * 查询不存在的页面编码应返回空列表。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void findByPageCode_noContent_shouldReturnEmptyList() throws Exception {
        mockMvc.perform(get("/api/v1/sys/help")
                        .param("pageCode", "test-nonexistent-page")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    /**
     * POST 新增帮助内容应返回 200 且响应体包含 helpId 和正确的 pageCode。
     *
     * @throws Exception MockMvc 请求异常
     */
    @Test
    void create_shouldAddHelpContent() throws Exception {
        HelpCreateRequest req = new HelpCreateRequest();
        req.setPageCode("test-create-page");
        req.setTitle("新增帮助标题");
        req.setSummary("新增帮助摘要");
        req.setContent("新增帮助详细内容");

        MvcResult result = mockMvc.perform(post("/api/v1/sys/help")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.helpId").isNotEmpty())
                .andExpect(jsonPath("$.data.pageCode").value("test-create-page"))
                .andExpect(jsonPath("$.data.title").value("新增帮助标题"))
                .andExpect(jsonPath("$.data.summary").value("新增帮助摘要"))
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        String helpId = data.path("helpId").asText();
        assertThat(helpId).isNotBlank();

        // 验证数据库中确实存在该条记录
        assertThat(helpRepository.findById(helpId)).isPresent();

        // 清理
        helpRepository.deleteById(helpId);
    }

    // ===== Helper =====

    /**
     * 向数据库插入一条帮助内容测试数据。
     *
     * @param helpId     帮助 ID
     * @param pageCode   页面编码
     * @param title      帮助标题
     * @param summary    简要描述
     * @param content    详细内容
     * @param sortOrder  排序值
     * @param helpStatus 帮助状态
     */
    private void insertHelp(final String helpId, final String pageCode,
                             final String title, final String summary,
                             final String content, final int sortOrder,
                             final String helpStatus) {
        SysHelpContent entity = new SysHelpContent();
        entity.setHelpId(helpId);
        entity.setPageCode(pageCode);
        entity.setTitle(title);
        entity.setSummary(summary);
        entity.setContent(content);
        entity.setSortOrder(sortOrder);
        entity.setHelpStatus(helpStatus);
        LocalDateTime now = LocalDateTime.now();
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
        helpRepository.save(entity);
    }
}
