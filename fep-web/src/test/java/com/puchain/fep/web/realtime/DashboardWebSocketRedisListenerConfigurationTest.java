package com.puchain.fep.web.realtime;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link DashboardWebSocketRedisListenerConfiguration} 单元测试：直接调用 {@code @Bean}
 * 方法验证胶水逻辑，<b>不启动</b> {@code SmartLifecycle} 容器（无需真 Redis）。
 */
class DashboardWebSocketRedisListenerConfigurationTest {

    private final DashboardWebSocketRedisListenerConfiguration config =
            new DashboardWebSocketRedisListenerConfiguration();

    @Test
    void messageListener_routesChannelBodyToRegistryOnMessage() {
        final RedisPubSubSessionRegistry registry = mock(RedisPubSubSessionRegistry.class);
        final MessageListener listener = config.dashboardWsMessageListener(registry);
        final String body = "{\"userId\":\"u1\",\"payload\":\"{}\"}";
        final Message message = mock(Message.class);
        when(message.getBody()).thenReturn(body.getBytes(StandardCharsets.UTF_8));

        listener.onMessage(message, null);

        verify(registry).onMessage(body);
    }

    @Test
    void listenerContainer_isBuiltWithGivenConnectionFactory() {
        final RedisConnectionFactory cf = mock(RedisConnectionFactory.class);
        final MessageListener listener = mock(MessageListener.class);

        final RedisMessageListenerContainer container = config.dashboardWsListenerContainer(cf, listener);

        assertThat(container).isNotNull();
        assertThat(container.getConnectionFactory()).isSameAs(cf);
    }
}
