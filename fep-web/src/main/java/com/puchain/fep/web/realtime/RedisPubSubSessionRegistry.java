package com.puchain.fep.web.realtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.puchain.fep.common.util.LogSanitizer;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.Objects;

/**
 * 多实例版 {@link WebSocketSessionRegistry}（DEF-1，build-ahead）。
 *
 * <p>WebSocket 会话与某个进程实例绑定，无法跨实例直接 {@code sendMessage}。本实现
 * 组合一份 {@link InMemorySessionRegistry}（{@link #local}）持<b>本实例</b>会话，并经
 * Redis 频道 {@value #CHANNEL} 把 {@code sendToUser} 广播到集群所有实例：</p>
 *
 * <ul>
 *   <li>{@link #register}/{@link #unregister}/{@link #sessionCount} 直接委托 {@link #local}
 *       （仅管本实例会话）。</li>
 *   <li>{@link #sendToUser} <b>只发布</b>到频道，<b>不</b>直接本地投递。</li>
 *   <li>{@link #onMessage} 收到频道消息（含本实例自己发布的）后投递到 {@link #local} 会话。</li>
 * </ul>
 *
 * <p><b>无重复投递</b>：Redis pub/sub 把每条消息投给<b>所有</b>订阅者（含发布者本实例）。
 * 故发布者不本地直投，所有实例（含自己）经 {@link #onMessage} 统一投递本地会话——天然
 * exactly-once-per-instance，无需 instanceId 去重。</p>
 *
 * <p>本类<b>不带</b> Spring stereotype；由 {@link DashboardWebSocketRegistryConfiguration}
 * 在 {@code fep.dashboard.ws.registry=redis} 时装配，并注册
 * {@code RedisMessageListenerContainer} 把频道消息路由到 {@link #onMessage}。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SuppressFBWarnings(value = "CRLF_INJECTION_LOGS",
        justification = "userId passed through LogSanitizer.sanitize() prior to LOG; payload is server-generated JSON")
public final class RedisPubSubSessionRegistry implements WebSocketSessionRegistry {

    /** 跨实例广播频道名。 */
    public static final String CHANNEL = "fep:dashboard:ws";

    private static final Logger LOG = LoggerFactory.getLogger(RedisPubSubSessionRegistry.class);
    private static final String FIELD_USER_ID = "userId";
    private static final String FIELD_PAYLOAD = "payload";

    /** 本实例会话与投递逻辑（仅管本进程会话）。 */
    private final InMemorySessionRegistry local;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * @param local         本实例内存会话注册表（非 null，每实例独立）
     * @param redisTemplate 跨实例发布用 Redis 模板（Spring bean）
     * @param objectMapper  JSON 编解码（Spring bean）
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "local is a per-instance registry owned by this bean; redisTemplate/objectMapper are "
                    + "Spring-managed singletons stored by reference per container contract")
    public RedisPubSubSessionRegistry(final InMemorySessionRegistry local,
            final StringRedisTemplate redisTemplate, final ObjectMapper objectMapper) {
        this.local = Objects.requireNonNull(local, "local");
        this.redisTemplate = Objects.requireNonNull(redisTemplate, "redisTemplate");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    @Override
    public void register(final String userId, final WebSocketSession session) {
        local.register(userId, session);
    }

    @Override
    public void unregister(final WebSocketSession session) {
        local.unregister(session);
    }

    /**
     * 广播到频道（不本地直投）；目标用户的会话由各实例 {@link #onMessage} 投递。
     *
     * @param userId  目标用户 id（非 null）
     * @param payload 文本载荷（非 null）
     */
    @Override
    public void sendToUser(final String userId, final String payload) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(payload, "payload");
        final String envelope;
        try {
            envelope = objectMapper.writeValueAsString(
                    Map.of(FIELD_USER_ID, userId, FIELD_PAYLOAD, payload));
        } catch (final JsonProcessingException ex) {
            LOG.warn("WS cross-instance publish serialization failed userId={} reason={}",
                    LogSanitizer.sanitize(userId),
                    LogSanitizer.sanitize(ex.getClass().getSimpleName()));
            return;
        }
        redisTemplate.convertAndSend(CHANNEL, envelope);
    }

    /**
     * 频道消息回调：解出 {@code {userId,payload}} 并投递到本实例会话
     * （由 {@code RedisMessageListenerContainer} 在收到 {@value #CHANNEL} 消息时调用）。
     *
     * @param body 频道消息体（JSON 信封字符串）
     */
    public void onMessage(final String body) {
        final JsonNode node;
        try {
            node = objectMapper.readTree(body);
        } catch (final JsonProcessingException ex) {
            LOG.warn("WS cross-instance message parse failed reason={}",
                    LogSanitizer.sanitize(ex.getClass().getSimpleName()));
            return;
        }
        final String userId = node.path(FIELD_USER_ID).asText("");
        final String payload = node.path(FIELD_PAYLOAD).asText("");
        if (userId.isBlank()) {
            return; // 信封缺 userId，忽略
        }
        local.sendToUser(userId, payload);
    }

    @Override
    public int sessionCount() {
        return local.sessionCount();
    }
}
