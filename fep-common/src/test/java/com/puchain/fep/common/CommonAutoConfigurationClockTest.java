package com.puchain.fep.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * {@link CommonAutoConfiguration} 提供的 canonical 共享 {@link Clock} bean 装配测试。
 *
 * <p>验证：① 默认提供单一系统 {@link Clock} bean；② {@code @ConditionalOnMissingBean}
 * 让位给测试/上层自定义 {@link Clock}（保障单测可喂 {@link Clock#fixed}）。</p>
 */
class CommonAutoConfigurationClockTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(CommonAutoConfiguration.class);

    @Test
    void providesSingleSystemClockBeanByDefault() {
        runner.run(ctx -> {
            assertThat(ctx).hasSingleBean(Clock.class);
            assertThat(ctx.getBean(Clock.class)).isNotNull();
        });
    }

    @Test
    void backsOffWhenCustomClockPresent() {
        final Clock fixed = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC);
        runner.withBean("customClock", Clock.class, () -> fixed).run(ctx -> {
            assertThat(ctx).hasSingleBean(Clock.class);
            assertThat(ctx.getBean(Clock.class)).isSameAs(fixed);
        });
    }
}
