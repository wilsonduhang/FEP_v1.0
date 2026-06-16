package com.puchain.fep.web.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * {@link DashboardWebSocketRegistryConfiguration} 注册表选择测试。
 *
 * <p>用 {@link ApplicationContextRunner} 验证 {@code fep.dashboard.ws.registry} 的
 * {@code @ConditionalOnProperty} 互斥装配，<b>无需真 Redis</b>（mock
 * {@link StringRedisTemplate}）。跨实例订阅的 {@code RedisMessageListenerContainer} 由
 * {@link DashboardWebSocketRedisListenerConfiguration} 分离装配，不在本测试范围——故此处
 * 不会触发 {@code SmartLifecycle} 容器启动。</p>
 */
class DashboardWebSocketRegistryConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(DashboardWebSocketRegistryConfiguration.class)
            .withBean(ObjectMapper.class, ObjectMapper::new)
            .withBean(StringRedisTemplate.class, () -> mock(StringRedisTemplate.class));

    @Test
    void defaultProperty_wiresInMemoryRegistry_andNoRedisRegistry() {
        runner.run(ctx -> {
            assertThat(ctx).hasSingleBean(WebSocketSessionRegistry.class);
            assertThat(ctx.getBean(WebSocketSessionRegistry.class))
                    .isInstanceOf(InMemorySessionRegistry.class);
            assertThat(ctx).doesNotHaveBean(RedisPubSubSessionRegistry.class);
        });
    }

    @Test
    void memoryProperty_wiresInMemoryRegistry() {
        runner.withPropertyValues("fep.dashboard.ws.registry=memory").run(ctx -> {
            assertThat(ctx.getBean(WebSocketSessionRegistry.class))
                    .isInstanceOf(InMemorySessionRegistry.class);
            assertThat(ctx).doesNotHaveBean(RedisPubSubSessionRegistry.class);
        });
    }

    @Test
    void redisProperty_wiresRedisPubSubRegistry_notInMemory() {
        runner.withPropertyValues("fep.dashboard.ws.registry=redis").run(ctx -> {
            assertThat(ctx.getBean(WebSocketSessionRegistry.class))
                    .isInstanceOf(RedisPubSubSessionRegistry.class);
            // memory bean 未装配（互斥）。
            assertThat(ctx).doesNotHaveBean(InMemorySessionRegistry.class);
        });
    }
}
