package com.puchain.fep.web.realtime;

import com.puchain.fep.common.util.LogSanitizer;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 单实例内存版 {@link WebSocketSessionRegistry} 实现。
 *
 * <p>用 {@code ConcurrentHashMap<userId, Set<WebSocketSession>>} 维护会话，
 * 同一用户可有多条会话（多标签页/多设备）。推送时对每条会话 {@code synchronized}
 * 串行化 {@code sendMessage}（{@link WebSocketSession} 不保证并发发送安全）。</p>
 *
 * <p><b>TODO（P2，多实例 deferred）</b>：当前注册表仅在单实例进程内有效。多实例
 * 部署时一个用户的会话可能分散在不同实例，需替换为 {@code RedisPubSubSessionRegistry}
 * ——本地内存保活会话 + Redis pub/sub 跨实例广播 {@code sendToUser} 消息。接口
 * {@link WebSocketSessionRegistry} 已为此预留替换点（muzhou 2026-06-15 签字：单实例 MVP）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
@SuppressFBWarnings(value = "CRLF_INJECTION_LOGS",
        justification = "userId/reason passed through LogSanitizer.sanitize() prior to LOG; sessionId is framework-generated")
public class InMemorySessionRegistry implements WebSocketSessionRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(InMemorySessionRegistry.class);

    /** userId → 该用户的活跃会话集合（线程安全 Set）。 */
    private final Map<String, Set<WebSocketSession>> sessionsByUser = new ConcurrentHashMap<>();

    @Override
    public void register(final String userId, final WebSocketSession session) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(session, "session");
        sessionsByUser.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(session);
    }

    @Override
    public void unregister(final WebSocketSession session) {
        Objects.requireNonNull(session, "session");
        sessionsByUser.values().forEach(set -> set.remove(session));
        sessionsByUser.entrySet().removeIf(e -> e.getValue().isEmpty());
    }

    @Override
    public void sendToUser(final String userId, final String payload) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(payload, "payload");
        final Set<WebSocketSession> sessions = sessionsByUser.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        final TextMessage frame = new TextMessage(payload);
        for (final WebSocketSession session : sessions) {
            if (!session.isOpen()) {
                sessions.remove(session);
                continue;
            }
            try {
                synchronized (session) {
                    session.sendMessage(frame);
                }
            } catch (final IOException ex) {
                LOG.warn("WS push failed, dropping session userId={} sessionId={} reason={}",
                        LogSanitizer.sanitize(userId), session.getId(),
                        LogSanitizer.sanitize(ex.getClass().getSimpleName()));
                sessions.remove(session);
            }
        }
    }

    @Override
    public int sessionCount() {
        return sessionsByUser.values().stream().mapToInt(Set::size).sum();
    }
}
