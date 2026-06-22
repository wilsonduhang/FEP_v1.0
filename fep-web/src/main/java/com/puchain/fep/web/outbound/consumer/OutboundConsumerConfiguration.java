package com.puchain.fep.web.outbound.consumer;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Outbound consumer 包配置：注册 {@link OutboundQueueProperties}。
 *
 * <p>共享 {@link java.time.Clock} bean 已于 2026-06-22 收敛至
 * {@code com.puchain.fep.common.CommonAutoConfiguration#systemClock()}（canonical
 * module-agnostic 归属），本类不再定义 Clock bean，消费方按类型注入共享 Clock。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Configuration
@EnableConfigurationProperties(OutboundQueueProperties.class)
public class OutboundConsumerConfiguration {
}
