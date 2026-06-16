package com.puchain.fep.web.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Dashboard WebSocket 会话注册表装配（DEF-1）。
 *
 * <p>由 {@code fep.dashboard.ws.registry} 择一装配 {@link WebSocketSessionRegistry}：</p>
 * <ul>
 *   <li><b>{@code memory}（默认，{@code matchIfMissing}）</b> → {@link InMemorySessionRegistry}，
 *       单实例进程内有效；当前部署形态。</li>
 *   <li><b>{@code redis}</b> → {@link RedisPubSubSessionRegistry}；跨实例广播的
 *       {@code RedisMessageListenerContainer} 由 {@link DashboardWebSocketRedisListenerConfiguration}
 *       装配（分离以便注册表选择可独立于 {@code SmartLifecycle} 容器测试）。多实例部署时显式开启。</li>
 * </ul>
 *
 * <p>两实现均<b>无</b> Spring stereotype，统一经本 {@code @Configuration} 的
 * {@code @Bean} + {@code @ConditionalOnProperty} 互斥装配——避免 {@code @ComponentScan}
 * 广扫绕过条件门致双 bean 或命名约束冲突（红线
 * provider_switch_impl_no_stereotype_bean_registration）。默认 {@code memory} 保证零生产
 * 行为变更（DEF-1 build-ahead：建好但默认不启用）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Configuration
public class DashboardWebSocketRegistryConfiguration {

    /** 注册表 provider 属性名。 */
    static final String REGISTRY_PROPERTY = "fep.dashboard.ws.registry";

    /**
     * 单实例内存注册表（默认）。
     *
     * @return 内存版 {@link WebSocketSessionRegistry}
     */
    @Bean
    @ConditionalOnProperty(name = REGISTRY_PROPERTY, havingValue = "memory", matchIfMissing = true)
    public WebSocketSessionRegistry inMemorySessionRegistry() {
        return new InMemorySessionRegistry();
    }

    /**
     * 多实例 Redis pub/sub 注册表（{@code fep.dashboard.ws.registry=redis} 时）。
     *
     * <p><b>返回具体类型</b>（非接口 {@link WebSocketSessionRegistry}）是<b>有意</b>：
     * {@link DashboardWebSocketRedisListenerConfiguration#dashboardWsMessageListener} 须按
     * 具体类型注入本 bean 以调用接口未声明的 {@link RedisPubSubSessionRegistry#onMessage}。
     * 改为接口返回会破坏该跨配置按类型注入。</p>
     *
     * @param redisTemplate 跨实例发布用 Redis 模板
     * @param objectMapper  JSON 编解码
     * @return Redis pub/sub 版注册表
     */
    @Bean
    @ConditionalOnProperty(name = REGISTRY_PROPERTY, havingValue = "redis")
    public RedisPubSubSessionRegistry redisPubSubSessionRegistry(final StringRedisTemplate redisTemplate,
            final ObjectMapper objectMapper) {
        return new RedisPubSubSessionRegistry(new InMemorySessionRegistry(), redisTemplate, objectMapper);
    }
}
