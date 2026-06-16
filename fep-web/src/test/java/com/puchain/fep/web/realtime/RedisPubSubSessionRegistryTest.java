package com.puchain.fep.web.realtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * {@link RedisPubSubSessionRegistry} 单元测试：发布广播 / onMessage 本地投递 /
 * 委托本地 / 无重复投递 / 信封边界。用 mock {@link StringRedisTemplate} + 真实
 * {@link InMemorySessionRegistry} local，无需真 Redis。
 */
class RedisPubSubSessionRegistryTest {

    private InMemorySessionRegistry local;
    private StringRedisTemplate redisTemplate;
    private ObjectMapper objectMapper;
    private RedisPubSubSessionRegistry registry;

    @BeforeEach
    void setUp() {
        local = new InMemorySessionRegistry();
        redisTemplate = mock(StringRedisTemplate.class);
        objectMapper = new ObjectMapper();
        registry = new RedisPubSubSessionRegistry(local, redisTemplate, objectMapper);
    }

    private WebSocketSession openSession(final String id) {
        final WebSocketSession s = mock(WebSocketSession.class);
        when(s.getId()).thenReturn(id);
        when(s.isOpen()).thenReturn(true);
        return s;
    }

    @Test
    void sendToUser_publishesEnvelopeToChannel_andDoesNotDeliverLocallyDirectly() throws Exception {
        // 本实例有该用户的活跃会话，但 sendToUser 只发布、不直投（避免重复——本实例也订阅频道）。
        final WebSocketSession s1 = openSession("s1");
        registry.register("user-1", s1);

        registry.sendToUser("user-1", "{\"type\":\"notification\",\"notificationId\":\"n1\"}");

        final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(redisTemplate).convertAndSend(eq(RedisPubSubSessionRegistry.CHANNEL), captor.capture());
        final JsonNode envelope = objectMapper.readTree(captor.getValue());
        assertThat(envelope.path("userId").asText()).isEqualTo("user-1");
        assertThat(envelope.path("payload").asText()).contains("notificationId");
        // 关键：无本地直投（投递只发生在 onMessage 收到频道回环时）。
        verify(s1, never()).sendMessage(any());
    }

    @Test
    void onMessage_deliversPayloadToLocalSessionsOfThatUser() throws Exception {
        final WebSocketSession s1 = openSession("s1");
        registry.register("user-1", s1);
        final String envelope = objectMapper.writeValueAsString(
                java.util.Map.of("userId", "user-1", "payload", "{\"type\":\"notification\"}"));

        registry.onMessage(envelope);

        verify(s1).sendMessage(any(TextMessage.class));
    }

    @Test
    void onMessage_withNoLocalSessionForUser_isSilentNoOp() {
        final String envelope = "{\"userId\":\"ghost\",\"payload\":\"{}\"}";
        registry.onMessage(envelope); // 本实例无该用户会话 → 静默跳过，不抛
        assertThat(registry.sessionCount()).isZero();
    }

    @Test
    void onMessage_blankUserId_isIgnored() throws Exception {
        final WebSocketSession s1 = openSession("s1");
        registry.register("user-1", s1);
        final String envelope = "{\"userId\":\"\",\"payload\":\"{}\"}";

        registry.onMessage(envelope);

        verify(s1, never()).sendMessage(any()); // 缺 userId 忽略
    }

    @Test
    void onMessage_blankPayload_isIgnored() throws Exception {
        final WebSocketSession s1 = openSession("s1");
        registry.register("user-1", s1);
        final String envelope = "{\"userId\":\"user-1\",\"payload\":\"\"}";

        registry.onMessage(envelope);

        verify(s1, never()).sendMessage(any()); // 缺 payload 不推空帧
    }

    @Test
    void onMessage_malformedJson_isIgnored() {
        registry.onMessage("not-json"); // 不抛
        assertThat(registry.sessionCount()).isZero();
    }

    @Test
    void register_unregister_sessionCount_delegateToLocal() {
        final WebSocketSession s1 = openSession("s1");
        registry.register("user-1", s1);
        assertThat(registry.sessionCount()).isEqualTo(1);
        assertThat(local.sessionCount()).isEqualTo(1); // 委托同一 local

        registry.unregister(s1);
        assertThat(registry.sessionCount()).isZero();
    }

    @Test
    void register_doesNotPublish() {
        final WebSocketSession s1 = openSession("s1");
        registry.register("user-1", s1);
        // 注册是本地操作，不触发任何 Redis 交互。
        verifyNoInteractions(redisTemplate);
    }
}
