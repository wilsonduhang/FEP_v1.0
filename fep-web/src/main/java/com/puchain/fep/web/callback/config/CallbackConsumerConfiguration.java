package com.puchain.fep.web.callback.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 回调队列消费配置注册。仅启用 {@link CallbackQueueProperties} 绑定。
 *
 * <p><strong>不</strong>定义 {@code Clock} bean —— 全局唯一
 * {@code com.puchain.fep.common.CommonAutoConfiguration#systemClock()}
 * （{@code @ConditionalOnMissingBean(Clock.class)}）已提供，
 * {@code CallbackRetryHandler} 直接注入复用，避免重复 bean。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Configuration
@EnableConfigurationProperties(CallbackQueueProperties.class)
public class CallbackConsumerConfiguration {
}
