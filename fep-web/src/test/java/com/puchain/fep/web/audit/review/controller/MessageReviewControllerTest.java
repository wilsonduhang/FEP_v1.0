package com.puchain.fep.web.audit.review.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.puchain.fep.common.security.PasswordHasher;
import com.puchain.fep.web.audit.review.domain.ReviewStatus;
import com.puchain.fep.web.audit.review.repository.MessageReviewTaskRepository;
import com.puchain.fep.web.audit.review.service.MessageReviewTaskService;
import com.puchain.fep.web.auth.domain.CaptchaResponse;
import com.puchain.fep.web.auth.domain.LoginRequest;
import com.puchain.fep.web.auth.service.CaptchaService;
import com.puchain.fep.web.config.TestRedisConfiguration;
import com.puchain.fep.web.sysmgmt.user.domain.SysUser;
import com.puchain.fep.web.sysmgmt.user.repository.SysUserRepository;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * {@link MessageReviewController} 集成测试（真 JWT 登录 + URL 安全规则）。
 *
 * <p>沿用 {@code BizMessageRecordControllerTest} 登录范式；非事务 + deleteAll 清理审核任务
 * （createFromFailedRecord 为 REQUIRES_NEW 独立提交）。验证列表/详情/通过/驳回 + 401 未授权。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@AutoConfigureMockMvc
class MessageReviewControllerTest {

    private static final String TEST_ACCOUNT = "auditadmin";
    private static final String TEST_PASSWORD = "admin@FEP2026";
    private static final String ADMIN_USER_ID = "00000000000000000000000000000001";
    private static final String BASE_URL = "/api/v1/audit/reviews";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private CaptchaService captchaService;
    @Autowired
    private SysUserRepository userRepository;
    @Autowired
    private PasswordHasher passwordHasher;
    @Autowired
    private MessageReviewTaskService reviewService;
    @Autowired
    private MessageReviewTaskRepository reviewRepository;

    private String accessToken;
    private String originalAccount;
    private String originalPasswordHash;

    @BeforeEach
    void setUp() throws Exception {
        TestRedisConfiguration.getStore().clear();
        reviewRepository.deleteAll();

        final SysUser admin = userRepository.findById(ADMIN_USER_ID).orElseThrow();
        originalAccount = admin.getUserAccount();
        originalPasswordHash = admin.getPasswordHash();
        admin.setUserAccount(TEST_ACCOUNT);
        admin.setPasswordHash(passwordHasher.hash(TEST_PASSWORD));
        admin.setLoginFailCount(0);
        admin.setLockedUntil(null);
        userRepository.save(admin);

        final CaptchaResponse cap = captchaService.generate();
        final String captchaCode = TestRedisConfiguration.getStore()
                .get("fep:captcha:" + cap.getCaptchaId());

        final LoginRequest loginReq = new LoginRequest();
        loginReq.setAccount(TEST_ACCOUNT);
        loginReq.setPassword(TEST_PASSWORD);
        loginReq.setCaptchaId(cap.getCaptchaId());
        loginReq.setCaptchaCode(captchaCode);

        final MvcResult loginResult = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();
        final JsonNode body = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        accessToken = body.path("data").path("accessToken").asText();
        assertFalse(accessToken.isBlank(), "accessToken should not be blank");
    }

    @AfterEach
    void tearDown() {
        final SysUser admin = userRepository.findById(ADMIN_USER_ID).orElseThrow();
        admin.setUserAccount(originalAccount);
        admin.setPasswordHash(originalPasswordHash);
        admin.setLoginFailCount(0);
        admin.setLockedUntil(null);
        userRepository.save(admin);
        reviewRepository.deleteAll();
        TestRedisConfiguration.getStore().clear();
    }

    private String createPendingTask(final String recordId) {
        reviewService.createFromFailedRecord(recordId, "1001", "txn-" + recordId, "PROC_8507", "field X invalid");
        return reviewRepository.findByMessageRecordId(recordId).orElseThrow().getReviewId();
    }

    @Test
    void list_returns200WithPendingTask() throws Exception {
        createPendingTask("rec-c1");

        mockMvc.perform(get(BASE_URL).param("status", "PENDING")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.pageNum").value(1))
                .andExpect(jsonPath("$.data.records[0].reviewStatus").value("PENDING"));
    }

    @Test
    void approve_returns200AndMarksApproved() throws Exception {
        final String id = createPendingTask("rec-c2");

        mockMvc.perform(put(BASE_URL + "/" + id + "/approve")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("comment", "looks fine"))))
                .andExpect(status().isOk());

        final var t = reviewRepository.findById(id).orElseThrow();
        assertThat(t.getReviewStatus()).isEqualTo(ReviewStatus.APPROVED.name());
        assertThat(t.getReviewerId()).isNotBlank();
    }

    @Test
    void reject_withReason_returns200AndMarksRejected() throws Exception {
        final String id = createPendingTask("rec-c3");

        mockMvc.perform(put(BASE_URL + "/" + id + "/reject")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("comment", "amount invalid"))))
                .andExpect(status().isOk());

        assertThat(reviewRepository.findById(id).orElseThrow().getReviewStatus())
                .isEqualTo(ReviewStatus.REJECTED.name());
    }

    @Test
    void reject_blankReason_returns400() throws Exception {
        final String id = createPendingTask("rec-c4");

        mockMvc.perform(put(BASE_URL + "/" + id + "/reject")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("comment", "   "))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void detail_unknownId_returns404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/no-such-id")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void list_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isUnauthorized());
    }
}
