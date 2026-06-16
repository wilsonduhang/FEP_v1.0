package com.puchain.fep.web.realtime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
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

    // ---- DEF-2 反向索引（sessionId→userId）不变量测试 ----

    @Test
    void unregister_removesSessionWithoutAffectingOtherUsers() {
        final WebSocketSession s1 = openSession("s1");
        final WebSocketSession s2 = openSession("s2");
        registry.register("user-1", s1);
        registry.register("user-2", s2);

        registry.unregister(s1);

        // 仅 user-2 的 s2 残留；反向 O(1) 定位不误删他人。
        assertThat(registry.sessionCount()).isEqualTo(1);
        // 不变量：反向索引与正向集恒等，无孤儿泄漏。
        assertThat(registry.reverseIndexSize()).isEqualTo(registry.sessionCount());
    }

    @Test
    void unregister_lastSessionOfUser_thenReRegisterSameSessionId_isConsistent() {
        final WebSocketSession s1 = openSession("s1");
        registry.register("user-1", s1);
        registry.unregister(s1);
        assertThat(registry.sessionCount()).isZero();

        // 反向条目须已清，重注册同 sessionId 不受残留干扰。
        registry.register("user-1", s1);
        assertThat(registry.sessionCount()).isEqualTo(1);
        assertThat(registry.reverseIndexSize()).isEqualTo(1);
    }

    @Test
    void sendToUser_droppingClosedSession_alsoClearsReverseIndex() {
        // 第一惰性丢弃点：isOpen()==false。
        final WebSocketSession closed = mock(WebSocketSession.class);
        when(closed.getId()).thenReturn("s1");
        when(closed.isOpen()).thenReturn(false);
        registry.register("user-1", closed);

        registry.sendToUser("user-1", "{}"); // 触发 isOpen()==false 丢弃

        // 丢弃点①须同步清反向索引——否则此处残留孤儿（sessionCount 漏检，reverseIndexSize 揪出）。
        assertThat(registry.reverseIndexSize()).isZero();
        // 反向索引已清：再 unregister 幂等不抛、count 一致。
        registry.unregister(closed);
        assertThat(registry.sessionCount()).isZero();
    }

    @Test
    void sendToUser_ioErrorDroppingSession_alsoClearsReverseIndex() throws Exception {
        // 第二惰性丢弃点：isOpen()==true 但 sendMessage 抛 IOException → catch 分支丢弃。
        final WebSocketSession s1 = openSession("s1"); // isOpen()==true
        doThrow(new IOException("boom")).when(s1).sendMessage(any());
        registry.register("user-1", s1);

        registry.sendToUser("user-1", "{}"); // 触发 IOException catch 丢弃

        // 丢弃点②须同步清反向索引（catch 分支），否则孤儿残留。
        assertThat(registry.reverseIndexSize()).isZero();
        registry.unregister(s1); // 反向索引已清，幂等不抛
        assertThat(registry.sessionCount()).isZero();
    }

    @Test
    void unregister_unknownSession_isSilentlyIgnored_andIdempotent() {
        final WebSocketSession unknown = openSession("nope");
        registry.unregister(unknown); // 未注册会话静默忽略，不抛
        registry.unregister(unknown); // 重复 unregister 仍幂等不抛
        assertThat(registry.sessionCount()).isZero();
        assertThat(registry.reverseIndexSize()).isZero();
    }
}
