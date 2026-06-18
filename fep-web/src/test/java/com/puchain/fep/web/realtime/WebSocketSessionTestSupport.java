package com.puchain.fep.web.realtime;

import org.springframework.web.socket.WebSocketSession;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * realtime 模块测试共享支撑：构造带固定 sessionId 的 mock {@link WebSocketSession}。
 *
 * <p>抽取自 {@code InMemorySessionRegistryTest} / {@code RedisPubSubSessionRegistryTest}
 * 两处逐字重复的 {@code openSession} 私有工厂 + {@code InMemorySessionRegistryTest} 内联的
 * closed-session 桩（Rule-of-Three 收敛，B-8 Simplify deferred drain）。两测试类均未启用
 * {@code MockitoExtension}，故 getId/isOpen 双桩即便某测试未触达 isOpen 也不触发
 * UnnecessaryStubbingException，抽取行为等价。
 */
final class WebSocketSessionTestSupport {

    private WebSocketSessionTestSupport() {
    }

    /**
     * 构造一个 {@code isOpen()==true} 的 mock 会话。
     *
     * @param id 会话 id（{@link WebSocketSession#getId()} 返回值）
     * @return open 状态的 mock 会话
     */
    static WebSocketSession openSession(final String id) {
        final WebSocketSession s = mock(WebSocketSession.class);
        when(s.getId()).thenReturn(id);
        when(s.isOpen()).thenReturn(true);
        return s;
    }

    /**
     * 构造一个 {@code isOpen()==false} 的 mock 会话（已关闭会话的丢弃/剪枝路径）。
     *
     * @param id 会话 id（{@link WebSocketSession#getId()} 返回值）
     * @return closed 状态的 mock 会话
     */
    static WebSocketSession closedSession(final String id) {
        final WebSocketSession s = mock(WebSocketSession.class);
        when(s.getId()).thenReturn(id);
        when(s.isOpen()).thenReturn(false);
        return s;
    }
}
