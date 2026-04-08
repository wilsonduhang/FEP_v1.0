package com.puchain.fep.web.dashboard.todo.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.puchain.fep.web.auth.domain.CaptchaResponse;
import com.puchain.fep.web.auth.domain.LoginRequest;
import com.puchain.fep.web.auth.service.CaptchaService;
import com.puchain.fep.web.config.TestRedisConfiguration;
import com.puchain.fep.web.dashboard.todo.repository.DashboardTodoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
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
 * Integration tests for {@link DashboardTodoController}.
 *
 * <p>Covers CRUD and status transition endpoints.
 * See PRD v1.3 section 5.2.2 (FR-WEB-DASH-TODO).</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@AutoConfigureMockMvc
class DashboardTodoControllerTest {

    private static final String TEST_ACCOUNT = "admins";
    private static final String TEST_PASSWORD = "admin@FEP2026";
    private static final String ADMIN_USER_ID = "00000000000000000000000000000001";
    private static final String BASE_URL = "/api/v1/dashboard/todos";

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
    private DashboardTodoRepository todoRepository;

    private String accessToken;
    private String originalAccount;
    private String originalPasswordHash;

    /**
     * Set up test account and login to obtain JWT token.
     *
     * @throws Exception on request failure
     */
    @BeforeEach
    void setUp() throws Exception {
        TestRedisConfiguration.getStore().clear();
        todoRepository.deleteAll();

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

        JsonNode body = objectMapper.readTree(
                loginResult.getResponse().getContentAsString());
        accessToken = body.path("data").path("accessToken").asText();
        assertFalse(accessToken.isBlank(), "accessToken should not be blank");
    }

    /**
     * Restore original admin credentials and clear Redis store.
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

        todoRepository.deleteAll();
        TestRedisConfiguration.getStore().clear();
    }

    /**
     * POST create with valid request should return 200 with todo data.
     *
     * @throws Exception on request failure
     */
    @Test
    void create_validRequest_shouldReturn200() throws Exception {
        Map<String, Object> request = Map.of(
                "title", "Test Todo",
                "taskType", "DATA_SUBMIT",
                "priority", "HIGH");

        mockMvc.perform(post(BASE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.todoId", notNullValue()))
                .andExpect(jsonPath("$.data.title", is("Test Todo")))
                .andExpect(jsonPath("$.data.todoStatus", is("PENDING")));
    }

    /**
     * POST create with invalid title (1 char) should return 400.
     *
     * @throws Exception on request failure
     */
    @Test
    void create_invalidTitle_shouldReturn400() throws Exception {
        Map<String, Object> request = Map.of(
                "title", "X",
                "taskType", "DATA_SUBMIT",
                "priority", "HIGH");

        mockMvc.perform(post(BASE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("PARAM_4002")));
    }

    /**
     * GET search should return paged results.
     *
     * @throws Exception on request failure
     */
    @Test
    void search_shouldReturnPagedResults() throws Exception {
        createTodo("Search Todo");

        mockMvc.perform(get(BASE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.total", greaterThanOrEqualTo(1)));
    }

    /**
     * GET count should return pending count.
     *
     * @throws Exception on request failure
     */
    @Test
    void countPending_shouldReturnCount() throws Exception {
        createTodo("Count Todo");

        mockMvc.perform(get(BASE_URL + "/count")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", greaterThanOrEqualTo(1)));
    }

    /**
     * PUT complete should mark todo as COMPLETED.
     *
     * @throws Exception on request failure
     */
    @Test
    void complete_shouldMarkCompleted() throws Exception {
        String todoId = createTodo("Complete Todo");

        mockMvc.perform(put(BASE_URL + "/" + todoId + "/complete")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.todoStatus", is("COMPLETED")));
    }

    /**
     * PUT complete on already completed todo should return 400 BIZ_5003.
     *
     * @throws Exception on request failure
     */
    @Test
    void complete_alreadyCompleted_shouldReturn400() throws Exception {
        String todoId = createTodo("Already Done");

        // Complete it first
        mockMvc.perform(put(BASE_URL + "/" + todoId + "/complete")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        // Complete again should fail
        mockMvc.perform(put(BASE_URL + "/" + todoId + "/complete")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("BIZ_5003")));
    }

    /**
     * DELETE should remove todo.
     *
     * @throws Exception on request failure
     */
    @Test
    void delete_shouldRemoveTodo() throws Exception {
        String todoId = createTodo("Delete Todo");

        mockMvc.perform(delete(BASE_URL + "/" + todoId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    // ===== Helper Methods =====

    private String createTodo(final String title) throws Exception {
        Map<String, Object> request = Map.of(
                "title", title,
                "taskType", "DATA_SUBMIT",
                "priority", "HIGH");

        MvcResult result = mockMvc.perform(post(BASE_URL)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(
                result.getResponse().getContentAsString())
                .at("/data/todoId").asText();
    }
}
