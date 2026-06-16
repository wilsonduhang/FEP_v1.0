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

    /**
     * sessionId → userId 反向索引（DEF-2）。
     *
     * <p>使 {@link #unregister} 由遍历全部用户会话集（O(U)）降为按 sessionId 直接
     * 定位归属用户（O(1)）。<b>不变量</b>：本表 key 集合恒等于 {@link #sessionsByUser}
     * 所有会话的 sessionId 集合——故 {@code register} / {@code unregister} /
     * {@code sendToUser} 两处惰性丢弃均须同步维护本表，缺一即漂移。</p>
     */
    private final Map<String, String> userIdBySessionId = new ConcurrentHashMap<>();

    @Override
    public void register(final String userId, final WebSocketSession session) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(session, "session");
        sessionsByUser.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(session);
        userIdBySessionId.put(session.getId(), userId);
    }

    @Override
    public void unregister(final WebSocketSession session) {
        Objects.requireNonNull(session, "session");
        final String userId = userIdBySessionId.remove(session.getId());
        if (userId == null) {
            return; // 未注册会话静默忽略（幂等契约）。
        }
        final Set<WebSocketSession> sessions = sessionsByUser.get(userId);
        if (sessions != null) {
            sessions.remove(session);
            // 原子清理空集条目（computeIfPresent 在 bin 锁内重映射，best-effort 等价旧 removeIf）。
            sessionsByUser.computeIfPresent(userId, (k, set) -> set.isEmpty() ? null : set);
        }
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
                userIdBySessionId.remove(session.getId()); // 同步反向索引（惰性丢弃点①）
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
                userIdBySessionId.remove(session.getId()); // 同步反向索引（惰性丢弃点②）
            }
        }
        // 丢弃可能清空该用户会话集，原子清理空条目（与 unregister 同语义）。
        sessionsByUser.computeIfPresent(userId, (k, set) -> set.isEmpty() ? null : set);
    }

    @Override
    public int sessionCount() {
        return sessionsByUser.values().stream().mapToInt(Set::size).sum();
    }

    /**
     * 反向索引当前条目数，用于测试/监控直接核验不变量。
     *
     * <p>正常状态下应恒等于 {@link #sessionCount()}——二者不等即反向索引与正向集
     * 发生漂移（孤儿条目泄漏）。仅 {@code sessionCount} 观测正向集会漏掉此类泄漏。</p>
     *
     * @return {@code userIdBySessionId} 条目数
     */
    int reverseIndexSize() {
        return userIdBySessionId.size();
    }
}
