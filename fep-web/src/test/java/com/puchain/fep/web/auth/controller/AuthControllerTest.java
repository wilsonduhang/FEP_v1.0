package com.puchain.fep.web.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.puchain.fep.web.auth.domain.CaptchaResponse;
import com.puchain.fep.web.auth.domain.LoginRequest;
import com.puchain.fep.web.auth.domain.LoginResponse;
import com.puchain.fep.web.auth.service.AuthService;
import com.puchain.fep.web.auth.service.CaptchaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AuthController 集成测试（SpringBootTest + MockMvc + Mock 服务层）。
 *
 * <p>验证 Controller 路由、请求校验、安全配置（公开路径 + 401 拦截）。
 * Redis 自动配置已排除，Redis 依赖的服务通过 MockBean 注入。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private CaptchaService captchaService;

    @Test
    void captchaEndpointShouldReturnOk() throws Exception {
        when(captchaService.generate()).thenReturn(
                new CaptchaResponse("test-id-123", "data:image/png;base64,abc", 300L));

        mockMvc.perform(get("/api/v1/auth/captcha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.captchaId").value("test-id-123"))
                .andExpect(jsonPath("$.data.imageBase64").value(startsWith("data:image/png;base64,")));
    }

    @Test
    void loginWithValidDataShouldReturnTokens() throws Exception {
        LoginResponse response = new LoginResponse(
                "access-token-xxx", "refresh-token-xxx",
                "user-id-001", "admin1", "管理员",
                List.of("SYSTEM_ADMIN"), false);
        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        LoginRequest req = new LoginRequest();
        req.setAccount("admin1");
        req.setPassword("admin@FEP2026");
        req.setCaptchaId("captcha-id-001");
        req.setCaptchaCode("ab12");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.accessToken").value("access-token-xxx"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token-xxx"))
                .andExpect(jsonPath("$.data.userAccount").value("admin1"));
    }

    @Test
    void loginWithBlankAccountShouldReturn400() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setAccount("");
        req.setPassword("admin@FEP2026");
        req.setCaptchaId("captcha-id-001");
        req.setCaptchaCode("ab12");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void protectedEndpointWithoutTokenShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_0401"));
    }

    @Test
    void logoutShouldReturnOk() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer some-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));
    }
}
