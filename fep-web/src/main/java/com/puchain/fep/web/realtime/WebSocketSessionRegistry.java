package com.puchain.fep.web.realtime;

import org.springframework.web.socket.WebSocketSession;

/**
 * Dashboard 实时推送的 WebSocket 会话注册表抽象。
 *
 * <p>按 {@code userId} 维护已认证的活跃会话集合，供通知推送时定位目标会话
 * （单向 server→client）。接口化是为后续多实例部署预留替换点：当前内存
 * 实现 {@link InMemorySessionRegistry} 仅适用单实例；多实例需 Redis pub/sub
 * 跨实例广播（P2，deferred，见实现类 TODO）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public interface WebSocketSessionRegistry {

    /**
     * 注册一条已认证会话到指定用户。
     *
     * @param userId  已认证用户 id（非 null/blank）
     * @param session 活跃 WebSocket 会话（非 null）
     */
    void register(String userId, WebSocketSession session);

    /**
     * 注销一条会话（连接关闭时调用）；幂等，未注册的会话静默忽略。
     *
     * @param session 待注销的 WebSocket 会话（非 null）
     */
    void unregister(WebSocketSession session);

    /**
     * 向指定用户的所有活跃会话推送文本载荷；用户无活跃会话时静默跳过。
     *
     * @param userId  目标用户 id（非 null）
     * @param payload 文本载荷（通常为 JSON 串，非 null）
     */
    void sendToUser(String userId, String payload);

    /**
     * @return 当前已注册的活跃会话总数（跨所有用户），用于监控/测试
     */
    int sessionCount();
}
