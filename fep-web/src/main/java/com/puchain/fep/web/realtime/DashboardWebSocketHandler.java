package com.puchain.fep.web.realtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.web.auth.jwt.JwtTokenProvider;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dashboard 关键告警实时推送的 WebSocket 处理器（单向 server→client）。
 *
 * <p><b>鉴权（muzhou 2026-06-15 签字方案 a — 首帧 token）</b>：浏览器原生
 * {@code WebSocket} 无法设自定义 Authorization header，故连接建立后客户端须发首帧
 * {@code {"type":"auth","token":"<jwt>"}}，服务端用 {@link JwtTokenProvider#parse}
 * 校验并取 {@code sub} 作 userId，成功则 {@link WebSocketSessionRegistry#register}；
 * 失败或在 {@code authTimeoutSeconds} 内未认证 → server 主动 close。</p>
 *
 * <p>未认证会话暂存于 {@link #pending}，由 {@link #sweepUnauthenticated()}
 * （{@code @Scheduled}，fep-web 全局 {@code @EnableScheduling}）定期清理，防资源泄漏。
 * 连接关闭 {@link #afterConnectionClosed} 原子注销会话。</p>
 *
 * <p>日志安全：token 绝不入日志；仅记 userId（经 {@link LogSanitizer}）与 sessionId
 * 及异常类型名（红线 logsanitizer + CRLF_INJECTION_LOGS）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
@SuppressFBWarnings(value = "CRLF_INJECTION_LOGS",
        justification = "userId/reason passed through LogSanitizer.sanitize() prior to LOG; token never logged")
public class DashboardWebSocketHandler extends TextWebSocketHandler {

    private static final Logger LOG = LoggerFactory.getLogger(DashboardWebSocketHandler.class);

    /** 会话属性 key：认证状态（Boolean）。 */
    static final String ATTR_AUTHENTICATED = "fep.ws.authenticated";
    /** 会话属性 key：连接建立时刻（Instant），用于超时清理。 */
    static final String ATTR_CONNECTED_AT = "fep.ws.connectedAt";
    /** 会话属性 key：认证后的 userId（String）。 */
    static final String ATTR_USER_ID = "fep.ws.userId";
    /** 首帧载荷防御上限（字节级宽松上界，防超大帧）。 */
    private static final int MAX_AUTH_FRAME_CHARS = 8192;
    private static final String AUTH_TYPE = "auth";

    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;
    private final WebSocketSessionRegistry registry;
    private final long authTimeoutSeconds;

    /** 未认证（pending）会话：sessionId → session。 */
    private final Map<String, WebSocketSession> pending = new ConcurrentHashMap<>();

    /**
     * 构造处理器。
     *
     * @param jwtTokenProvider   JWT 解析校验（Spring bean）
     * @param objectMapper       JSON 解析（Spring bean）
     * @param registry           会话注册表（Spring bean）
     * @param authTimeoutSeconds 首帧鉴权超时秒数（默认 60，可配置）
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Spring-managed singletons (provider/mapper/registry) stored by reference per container contract")
    public DashboardWebSocketHandler(final JwtTokenProvider jwtTokenProvider,
            final ObjectMapper objectMapper, final WebSocketSessionRegistry registry,
            @Value("${fep.dashboard.ws.auth-timeout-seconds:60}") final long authTimeoutSeconds) {
        this.jwtTokenProvider = Objects.requireNonNull(jwtTokenProvider, "jwtTokenProvider");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.registry = Objects.requireNonNull(registry, "registry");
        this.authTimeoutSeconds = authTimeoutSeconds;
    }

    @Override
    public void afterConnectionEstablished(final WebSocketSession session) {
        session.getAttributes().put(ATTR_AUTHENTICATED, Boolean.FALSE);
        session.getAttributes().put(ATTR_CONNECTED_AT, Instant.now());
        pending.put(session.getId(), session);
    }

    @Override
    protected void handleTextMessage(final WebSocketSession session, final TextMessage message) {
        if (Boolean.TRUE.equals(session.getAttributes().get(ATTR_AUTHENTICATED))) {
            // 已认证会话为单向推送，忽略客户端后续帧（不重复注册、不关闭）。
            return;
        }
        final String payload = message.getPayload();
        if (payload.length() > MAX_AUTH_FRAME_CHARS) {
            closeUnauthenticated(session, "oversized auth frame");
            return;
        }
        try {
            final JsonNode node = objectMapper.readTree(payload);
            final String type = node.path("type").asText("");
            final String token = node.path("token").asText("");
            if (!AUTH_TYPE.equals(type) || token.isBlank()) {
                closeUnauthenticated(session, "malformed auth frame");
                return;
            }
            final Claims claims = jwtTokenProvider.parse(token);
            final String userId = claims.getSubject();
            if (userId == null || userId.isBlank()) {
                closeUnauthenticated(session, "token without subject");
                return;
            }
            session.getAttributes().put(ATTR_AUTHENTICATED, Boolean.TRUE);
            session.getAttributes().put(ATTR_USER_ID, userId);
            pending.remove(session.getId());
            registry.register(userId, session);
            LOG.info("WS authenticated userId={} sessionId={}",
                    LogSanitizer.sanitize(userId), session.getId());
        } catch (final JsonProcessingException | JwtException | IllegalArgumentException ex) {
            // token 不入日志；仅记异常类型名（畸形 JSON 与非法 token 同等拒绝）。
            LOG.warn("WS auth rejected sessionId={} reason={}", session.getId(),
                    LogSanitizer.sanitize(ex.getClass().getSimpleName()));
            closeUnauthenticated(session, "invalid token or frame");
        }
    }

    @Override
    public void afterConnectionClosed(final WebSocketSession session, final CloseStatus status) {
        pending.remove(session.getId());
        registry.unregister(session);
    }

    /**
     * 定期清理超时未认证会话（防资源泄漏）；{@code @EnableScheduling} 已全局启用。
     *
     * <p>包级可见以便单元测试直接驱动（不依赖真实时钟等待）。</p>
     */
    @Scheduled(fixedDelayString = "${fep.dashboard.ws.sweep-interval-ms:30000}")
    void sweepUnauthenticated() {
        final Instant cutoff = Instant.now().minusSeconds(authTimeoutSeconds);
        for (final WebSocketSession session : pending.values()) {
            final Object connectedAt = session.getAttributes().get(ATTR_CONNECTED_AT);
            if (!(connectedAt instanceof Instant) || ((Instant) connectedAt).isBefore(cutoff)) {
                closeUnauthenticated(session, "auth timeout");
            }
        }
    }

    private void closeUnauthenticated(final WebSocketSession session, final String reason) {
        pending.remove(session.getId());
        try {
            session.close(CloseStatus.POLICY_VIOLATION);
        } catch (final IOException ex) {
            LOG.debug("WS close failed sessionId={} reason={}", session.getId(),
                    LogSanitizer.sanitize(reason));
        }
    }
}
