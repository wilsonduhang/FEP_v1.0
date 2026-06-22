package com.puchain.fep.common;

import java.time.Clock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * FEP Common 模块自动配置——公共 Bean 的 canonical 归属。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Configuration
public class CommonAutoConfiguration {

    /**
     * 全 app 共享的 canonical {@link Clock} bean（系统默认时区）。
     *
     * <p>时间敏感的生产代码应<strong>构造注入此共享 {@link Clock}</strong>（而非裸调
     * {@code Instant.now()}/{@code System.currentTimeMillis()}），以便单测喂
     * {@link Clock#fixed} 校验确定性时间逻辑。{@link ConditionalOnMissingBean}
     * 让位给测试或上层自定义 {@link Clock} bean。</p>
     *
     * <p>历史：此 bean 原错放于 {@code outbound/consumer/OutboundConsumerClockConfiguration}，
     * 2026-06-22 收敛至本 module-agnostic 归属（{@link Clock} 非 web/outbound 专属）。</p>
     *
     * @return 系统默认时区 {@link Clock}
     */
    @Bean
    @ConditionalOnMissingBean(Clock.class)
    public Clock systemClock() {
        return Clock.systemDefaultZone();
    }
}
