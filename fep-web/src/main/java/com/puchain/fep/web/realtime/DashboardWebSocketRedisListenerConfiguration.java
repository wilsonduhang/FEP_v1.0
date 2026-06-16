package com.puchain.fep.web.realtime;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.nio.charset.StandardCharsets;

/**
 * Dashboard WebSocket 跨实例广播订阅装配（DEF-1，仅 {@code fep.dashboard.ws.registry=redis}）。
 *
 * <p>注册 {@link RedisMessageListenerContainer} 订阅 {@link RedisPubSubSessionRegistry#CHANNEL}，
 * 把频道消息路由到 {@link RedisPubSubSessionRegistry#onMessage}（投递到本实例会话）。</p>
 *
 * <p>与 {@link DashboardWebSocketRegistryConfiguration}（注册表选择）<b>分离</b>：容器是
 * {@code SmartLifecycle}，refresh 时自启订阅真 Redis，无法在无 Redis 的轻量装配测试中启动；
 * 分离后注册表选择测试不触碰容器。{@link MessageListener} 单独 {@code @Bean} 抽出，使
 * “频道消息 → onMessage”的胶水逻辑可不启动容器即单测。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Configuration
@ConditionalOnProperty(name = DashboardWebSocketRegistryConfiguration.REGISTRY_PROPERTY, havingValue = "redis")
public class DashboardWebSocketRedisListenerConfiguration {

    /**
     * 频道消息监听器：解出消息体字符串转交 {@link RedisPubSubSessionRegistry#onMessage}。
     * 抽为独立 {@code @Bean} 以便不启动容器即单测胶水逻辑。
     *
     * @param registry 目标注册表
     * @return 消息监听器
     */
    @Bean
    public MessageListener dashboardWsMessageListener(final RedisPubSubSessionRegistry registry) {
        return (message, pattern) -> registry.onMessage(new String(message.getBody(), StandardCharsets.UTF_8));
    }

    /**
     * 订阅 {@link RedisPubSubSessionRegistry#CHANNEL} 的监听容器。
     *
     * @param connectionFactory       Redis 连接工厂
     * @param dashboardWsMessageListener 频道消息监听器（按 bean 名注入）
     * @return 消息监听容器
     */
    @Bean
    public RedisMessageListenerContainer dashboardWsListenerContainer(
            final RedisConnectionFactory connectionFactory, final MessageListener dashboardWsMessageListener) {
        final RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(dashboardWsMessageListener,
                new ChannelTopic(RedisPubSubSessionRegistry.CHANNEL));
        return container;
    }
}
