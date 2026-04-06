package com.puchain.fep.web.config;

import com.puchain.fep.web.auth.jwt.JwtAuthEntryPoint;
import com.puchain.fep.web.auth.jwt.JwtAuthFilter;
import com.puchain.fep.web.auth.jwt.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Spring Security 配置测试 — 验证公开路径和 401 拦截。
 *
 * <p>使用全量上下文（无 Redis），通过 {@code spring.autoconfigure.exclude}
 * 排除 Redis 自动配置，使 Redis 依赖的 bean 不会注册。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigurationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void protectedEndpointWithoutTokenShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_0401"));
    }

    @Test
    void swaggerUiShouldBePublic() throws Exception {
        // Swagger UI redirect should return 3xx or 200, not 401
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(result -> {
                    int httpStatus = result.getResponse().getStatus();
                    // Swagger might return 302 redirect or 200 or 404 (if not serving)
                    // but it should NOT be 401
                    org.junit.jupiter.api.Assertions.assertNotEquals(
                            401, httpStatus, "Swagger UI should be public");
                });
    }

    @Test
    void actuatorHealthShouldBePublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }
}
