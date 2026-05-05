package com.puchain.fep.web.outbound.consumer;

import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Outbound consumer 包内的默认 {@link Clock} bean 提供者。
 *
 * <p>{@link BodyMsgIdGenerator} 通过构造注入 {@link Clock} 以便单测可以喂入
 * {@link Clock#fixed} 校验确定性 datetime 前缀；生产环境则需要容器内有
 * 一个 {@link Clock} bean 才能完成 autowiring。</p>
 *
 * <p>使用 {@link ConditionalOnMissingBean} 让位给上层（如 fep-web 全局配置）
 * 自定义的 {@link Clock} bean，避免冲突。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Configuration
@EnableConfigurationProperties(OutboundQueueProperties.class)
public class OutboundConsumerClockConfiguration {

    /**
     * 默认 {@link Clock} bean，使用系统默认时区。
     *
     * <p>{@link BodyMsgIdGenerator} 内部通过 {@code clock.withZone(Asia/Shanghai)}
     * 强制时区，因此此处使用系统默认时区不影响正确性。</p>
     *
     * @return 默认 {@link Clock} 实例
     */
    @Bean
    @ConditionalOnMissingBean(Clock.class)
    public Clock systemClock() {
        return Clock.systemDefaultZone();
    }
}
