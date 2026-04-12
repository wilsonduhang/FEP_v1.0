package com.puchain.fep.web.config;

import com.puchain.fep.web.auth.jwt.JwtAuthEntryPoint;
import com.puchain.fep.web.auth.jwt.JwtAuthFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security 配置（无状态 JWT + CORS 白名单）。
 *
 * <p>CSRF 禁用（前后端分离 + 无状态 JWT），
 * CORS 通过 {@code fep.cors.allowed-origins} 配置白名单（禁止通配符）。</p>
 *
 * <p>公开路径：认证接口 + Swagger UI + H2 Console (dev) + Actuator health/info。
 * 其余路径必须携带有效 JWT。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    /** CORS preflight 缓存时间（秒）：1 小时。 */
    private static final long CORS_MAX_AGE_SECONDS = 3600L;

    // P6e.2 Task 2: replaced /api/v1/auth/** wildcard with explicit paths
    // so that /api/v1/auth/me requires JWT authentication.
    // /api/v1/auth/logout kept public: logout must work even with expired tokens.
    private static final String[] PUBLIC_PATHS = {
            "/api/v1/auth/login",
            "/api/v1/auth/captcha",
            "/api/v1/auth/public-key",
            "/api/v1/auth/refresh",
            "/api/v1/auth/logout",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/h2-console/**",
            "/actuator/health",
            "/actuator/info"
    };

    private final JwtAuthFilter jwtAuthFilter;
    private final JwtAuthEntryPoint jwtAuthEntryPoint;

    /**
     * CORS 允许的 Origin 列表（逗号分隔），通过 {@code fep.cors.allowed-origins} 注入。
     *
     * <p>dev: {@code http://localhost:*,http://127.0.0.1:*}</p>
     * <p>prod: 具体域名（如 {@code https://fep.example.com}），禁止通配符 {@code *}。</p>
     */
    @Value("${fep.cors.allowed-origins:http://localhost:*}")
    private String allowedOrigins;

    /**
     * 构造 SecurityConfiguration。
     *
     * @param jwtAuthFilter    JWT 认证过滤器
     * @param jwtAuthEntryPoint 401 响应处理器
     */
    public SecurityConfiguration(final JwtAuthFilter jwtAuthFilter,
                                 final JwtAuthEntryPoint jwtAuthEntryPoint) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.jwtAuthEntryPoint = jwtAuthEntryPoint;
    }

    /**
     * 构建安全过滤器链。
     *
     * @param http HttpSecurity 构建器
     * @return 安全过滤器链
     * @throws Exception 配置异常
     */
    @Bean
    public SecurityFilterChain filterChain(final HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(eh -> eh.authenticationEntryPoint(jwtAuthEntryPoint))
            .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(PUBLIC_PATHS).permitAll()
                    .anyRequest().authenticated())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * CORS 配置源（从 {@code fep.cors.allowed-origins} 读取白名单）。
     *
     * @return CORS 配置源
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        config.setAllowedOriginPatterns(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("X-Trace-Id", "Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(CORS_MAX_AGE_SECONDS);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
