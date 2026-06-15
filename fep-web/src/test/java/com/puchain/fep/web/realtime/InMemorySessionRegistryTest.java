package com.puchain.fep.web.realtime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link InMemorySessionRegistry} 单元测试：注册/推送/注销/边界。
 */
class InMemorySessionRegistryTest {

    private InMemorySessionRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new InMemorySessionRegistry();
    }

    private WebSocketSession openSession(final String id) {
        final WebSocketSession s = mock(WebSocketSession.class);
        when(s.getId()).thenReturn(id);
        when(s.isOpen()).thenReturn(true);
        return s;
    }

    @Test
    void sendToUser_deliversToAllOpenSessionsOfThatUser() throws Exception {
        final WebSocketSession s1 = openSession("a");
        final WebSocketSession s2 = openSession("b");
        registry.register("user-1", s1);
        registry.register("user-1", s2);

        registry.sendToUser("user-1", "{\"type\":\"notification\"}");

        verify(s1).sendMessage(any(TextMessage.class));
        verify(s2).sendMessage(any(TextMessage.class));
        assertThat(registry.sessionCount()).isEqualTo(2);
    }

    @Test
    void sendToUser_withNoSessions_isSilentNoOp() {
        // 不抛异常即通过（无目标会话静默跳过）。
        registry.sendToUser("ghost", "payload");
        assertThat(registry.sessionCount()).isZero();
    }

    @Test
    void sendToUser_skipsAndPrunesClosedSession() throws Exception {
        final WebSocketSession closed = mock(WebSocketSession.class);
        when(closed.getId()).thenReturn("c");
        when(closed.isOpen()).thenReturn(false);
        registry.register("user-2", closed);

        registry.sendToUser("user-2", "payload");

        verify(closed, never()).sendMessage(any());
        assertThat(registry.sessionCount()).isZero();
    }

    @Test
    void sendToUser_doesNotLeakToOtherUsers() throws Exception {
        final WebSocketSession s1 = openSession("a");
        final WebSocketSession other = openSession("z");
        registry.register("user-1", s1);
        registry.register("user-2", other);

        registry.sendToUser("user-1", "payload");

        verify(s1).sendMessage(any(TextMessage.class));
        verify(other, never()).sendMessage(any());
    }

    @Test
    void unregister_removesSessionAndCleansEmptyUserEntry() {
        final WebSocketSession s1 = openSession("a");
        registry.register("user-1", s1);
        assertThat(registry.sessionCount()).isEqualTo(1);

        registry.unregister(s1);

        assertThat(registry.sessionCount()).isZero();
    }
}
