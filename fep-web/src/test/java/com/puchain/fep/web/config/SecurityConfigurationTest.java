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
 * Spring Security 配置测试 — 验证公开路径（{@code /actuator/health}、Swagger UI）
 * 和受保护路径的 401 拦截。
 *
 * <p>使用全量上下文（dev profile，与本仓其他 controller test 一致 — 不显式
 * {@code @ActiveProfiles}）。Redis 自动配置保持加载（{@code CaptchaService}/
 * {@code LoginAttemptService}/{@code SingleSignOnService} 强依赖
 * {@code StringRedisTemplate}，无 {@code @Profile} 隔离），但 RedisHealthIndicator
 * 在测试中关闭（{@code management.health.redis.enabled=false}），避免
 * {@code /actuator/health} 因外部 Redis 不可达返回 503，把本测试退化为真正
 * "端点是否公开"的契约校验。生产配置不变。</p>
 *
 * <p>R1-DEFER-5：闭合 R1 期间 fep-web pre-existing failure（Plan §F-5），
 * 根因为测试对外部 Redis 的隐式依赖。修复点仅在测试 properties，主代码不动。</p>
 */
@SpringBootTest(properties = "management.health.redis.enabled=false")
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
