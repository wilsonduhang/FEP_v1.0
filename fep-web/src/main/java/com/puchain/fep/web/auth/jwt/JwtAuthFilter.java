package com.puchain.fep.web.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT 认证过滤器。
 *
 * <p>每次请求从 Authorization 头提取 Bearer token，验证后构造 SecurityContext。</p>
 *
 * <p>Redis 依赖为可选注入：当 Redis 不可用时（如测试环境），
 * 跳过黑名单和 SSO 校验，仅验证 token 签名和有效期。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);
    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";
    private static final String BLACKLIST_KEY = "fep:jwt:blacklist:";
    private static final String SESSION_KEY = "fep:session:";

    private final JwtTokenProvider tokenProvider;
    private final StringRedisTemplate redisTemplate;

    /**
     * 构造 JwtAuthFilter。
     *
     * @param tokenProvider JWT 签发/解析
     * @param redisTemplate Redis 模板（可选，测试环境可为 null）
     */
    public JwtAuthFilter(final JwtTokenProvider tokenProvider,
                         @Autowired(required = false) final StringRedisTemplate redisTemplate) {
        this.tokenProvider = tokenProvider;
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected void doFilterInternal(final HttpServletRequest request,
                                    final HttpServletResponse response,
                                    final FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);
        if (token != null) {
            try {
                Claims claims = tokenProvider.parse(token);
                String jti = claims.getId();
                String userId = claims.getSubject();

                if (redisTemplate == null) {
                    // Redis 不可用 — 仅验证签名，跳过黑名单/SSO
                    log.debug("Redis unavailable, skipping blacklist/SSO checks");
                    authenticate(claims, request);
                } else if (Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_KEY + jti))) {
                    // 1. 黑名单校验（登出后 token 失效）
                    log.debug("JWT {} is blacklisted", sanitize(jti));
                } else if (!isCurrentSession(userId, jti)) {
                    // 2. 单点踢出校验 (PRD §5.1.5)
                    log.debug("JWT {} is not current session for user {} (kicked out)",
                            sanitize(jti), sanitize(userId));
                } else {
                    authenticate(claims, request);
                }
            } catch (JwtException ex) {
                log.debug("JWT validation failed: {}", sanitize(ex.getMessage()));
                // 不抛，由 EntryPoint 处理未认证请求
            }
        }
        filterChain.doFilter(request, response);
    }

    /**
     * 校验当前 token 的 jti 是否为该用户最新会话的 jti。
     *
     * <p>AuthService.login() 在登录成功时写入 {@code fep:session:{userId} = jti}，
     * 若新登录发生，旧 jti 即被覆盖，旧 token 校验时失败。</p>
     *
     * <p>若 Redis 中无记录（如 refresh token 重新签发的 access），视为有效。</p>
     *
     * @param userId 用户 ID
     * @param jti    JWT ID
     * @return true 为当前会话
     */
    private boolean isCurrentSession(final String userId, final String jti) {
        String currentJti = redisTemplate.opsForValue().get(SESSION_KEY + userId);
        return currentJti == null || currentJti.equals(jti);
    }

    /**
     * 从请求头提取 Bearer token。
     *
     * @param request HTTP 请求
     * @return token 字符串或 null
     */
    private String extractToken(final HttpServletRequest request) {
        String header = request.getHeader(HEADER);
        if (header != null && header.startsWith(PREFIX)) {
            return header.substring(PREFIX.length());
        }
        return null;
    }

    /**
     * 从 Claims 构造 Spring Security 认证对象并设置到 SecurityContext。
     *
     * @param claims  JWT Claims
     * @param request HTTP 请求
     */
    @SuppressWarnings("unchecked")
    private void authenticate(final Claims claims, final HttpServletRequest request) {
        String userId = claims.getSubject();
        List<String> roleCodes = (List<String>) claims.getOrDefault("roles", List.of());
        List<SimpleGrantedAuthority> authorities = roleCodes.stream()
                .map(code -> new SimpleGrantedAuthority("ROLE_" + code))
                .collect(Collectors.toList());
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userId, null, authorities);
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    /**
     * 清除 CRLF 字符，防止日志注入。
     *
     * @param input 原始字符串
     * @return 安全字符串
     */
    private static String sanitize(final String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\r", "\\r").replace("\n", "\\n");
    }
}
