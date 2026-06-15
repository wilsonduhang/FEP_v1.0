package com.puchain.fep.web.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.puchain.fep.web.auth.jwt.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link DashboardWebSocketHandler} 单元测试：首帧鉴权、拒绝、超时清理、注销、单向忽略。
 */
@ExtendWith(MockitoExtension.class)
class DashboardWebSocketHandlerTest {

    private static final long AUTH_TIMEOUT_SECONDS = 60L;

    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private WebSocketSessionRegistry registry;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private DashboardWebSocketHandler handler;

    @BeforeEach
    void setUp() {
        handler = new DashboardWebSocketHandler(jwtTokenProvider, objectMapper, registry, AUTH_TIMEOUT_SECONDS);
    }

    private WebSocketSession newSession(final String id, final Map<String, Object> attrs) {
        final WebSocketSession session = mock(WebSocketSession.class);
        lenient().when(session.getId()).thenReturn(id);
        lenient().when(session.getAttributes()).thenReturn(attrs);
        return session;
    }

    @Test
    void validAuthFrame_registersSessionAndDoesNotClose() throws Exception {
        final Map<String, Object> attrs = new HashMap<>();
        final WebSocketSession session = newSession("s1", attrs);
        final Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("user-1");
        when(jwtTokenProvider.parse("good-token")).thenReturn(claims);

        handler.afterConnectionEstablished(session);
        handler.handleTextMessage(session, new TextMessage("{\"type\":\"auth\",\"token\":\"good-token\"}"));

        verify(registry).register(eq("user-1"), eq(session));
        verify(session, never()).close(any());
        assertThat(attrs.get(DashboardWebSocketHandler.ATTR_AUTHENTICATED)).isEqualTo(Boolean.TRUE);
        assertThat(attrs.get(DashboardWebSocketHandler.ATTR_USER_ID)).isEqualTo("user-1");
    }

    @Test
    void invalidToken_closesSessionAndDoesNotRegister() throws Exception {
        final Map<String, Object> attrs = new HashMap<>();
        final WebSocketSession session = newSession("s2", attrs);
        when(jwtTokenProvider.parse("bad-token")).thenThrow(new JwtException("bad signature"));

        handler.afterConnectionEstablished(session);
        handler.handleTextMessage(session, new TextMessage("{\"type\":\"auth\",\"token\":\"bad-token\"}"));

        verify(session).close(CloseStatus.POLICY_VIOLATION);
        verify(registry, never()).register(any(), any());
    }

    @Test
    void malformedFrame_closesSessionAndDoesNotRegister() throws Exception {
        final Map<String, Object> attrs = new HashMap<>();
        final WebSocketSession session = newSession("s3", attrs);

        handler.afterConnectionEstablished(session);
        handler.handleTextMessage(session, new TextMessage("{\"hello\":\"world\"}"));

        verify(session).close(CloseStatus.POLICY_VIOLATION);
        verify(registry, never()).register(any(), any());
    }

    @Test
    void blankSubjectToken_closesSession() throws Exception {
        final Map<String, Object> attrs = new HashMap<>();
        final WebSocketSession session = newSession("s4", attrs);
        final Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("  ");
        when(jwtTokenProvider.parse("subjectless")).thenReturn(claims);

        handler.afterConnectionEstablished(session);
        handler.handleTextMessage(session, new TextMessage("{\"type\":\"auth\",\"token\":\"subjectless\"}"));

        verify(session).close(CloseStatus.POLICY_VIOLATION);
        verify(registry, never()).register(any(), any());
    }

    @Test
    void timeoutSweep_closesStalePendingSession() throws Exception {
        final Map<String, Object> attrs = new HashMap<>();
        final WebSocketSession session = newSession("s5", attrs);
        handler.afterConnectionEstablished(session);
        // 模拟连接已建立 120s 未认证（远超 60s 超时）。
        attrs.put(DashboardWebSocketHandler.ATTR_CONNECTED_AT, Instant.now().minusSeconds(120));

        handler.sweepUnauthenticated();

        verify(session).close(CloseStatus.POLICY_VIOLATION);
    }

    @Test
    void timeoutSweep_keepsFreshPendingSession() throws Exception {
        final Map<String, Object> attrs = new HashMap<>();
        final WebSocketSession session = newSession("s6", attrs);
        handler.afterConnectionEstablished(session); // connectedAt = now

        handler.sweepUnauthenticated();

        verify(session, never()).close(any());
    }

    @Test
    void afterConnectionClosed_unregistersSession() {
        final Map<String, Object> attrs = new HashMap<>();
        final WebSocketSession session = newSession("s7", attrs);

        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        verify(registry).unregister(session);
    }

    @Test
    void alreadyAuthenticated_ignoresFurtherFrames() throws Exception {
        final Map<String, Object> attrs = new HashMap<>();
        attrs.put(DashboardWebSocketHandler.ATTR_AUTHENTICATED, Boolean.TRUE);
        final WebSocketSession session = newSession("s8", attrs);

        handler.handleTextMessage(session, new TextMessage("{\"type\":\"auth\",\"token\":\"whatever\"}"));

        verify(registry, never()).register(any(), any());
        verify(session, never()).close(any());
    }
}
