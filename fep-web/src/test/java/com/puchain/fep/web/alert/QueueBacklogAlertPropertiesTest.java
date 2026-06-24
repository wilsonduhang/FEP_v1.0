package com.puchain.fep.web.alert;

import com.puchain.fep.web.callback.repository.CallbackQueueRepository;
import com.puchain.fep.web.outbound.consumer.OutboundQueueRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * {@link QueueBacklogAlertProperties} 绑定 + {@link QueueBacklogMonitor} 条件装配测试（DEF-B9-3 T5）。
 *
 * <p>用 {@link ApplicationContextRunner} 验证 {@code fep.alert.queue-backlog} 前缀的默认/覆盖绑定
 * 与 {@code @ConditionalOnProperty(enabled, matchIfMissing=true)} 开关，<b>无需完整 Spring 上下文</b>
 * （mock 两 queue repo + Clock）。monitor 无 SmartLifecycle，无需拆配置（区别于
 * {@code DashboardWebSocketRegistryConfigurationTest}）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class QueueBacklogAlertPropertiesTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(QueueBacklogAlertConfiguration.class, QueueBacklogMonitor.class)
            .withBean(CallbackQueueRepository.class, () -> mock(CallbackQueueRepository.class))
            .withBean(OutboundQueueRepository.class, () -> mock(OutboundQueueRepository.class))
            .withBean(Clock.class, Clock::systemUTC);

    @Test
    void defaultBinding_usesDefaults() {
        runner.run(ctx -> {
            final QueueBacklogAlertProperties p = ctx.getBean(QueueBacklogAlertProperties.class);
            assertThat(p.enabled()).isTrue();
            assertThat(p.intervalMs()).isEqualTo(60000L);
            assertThat(p.threshold()).isEqualTo(1000L);
            assertThat(p.callbackEnabled()).isTrue();
            assertThat(p.outboundEnabled()).isTrue();
        });
    }

    @Test
    void explicitOverride_bindsFromPrefix() {
        runner.withPropertyValues(
                "fep.alert.queue-backlog.threshold=5000",
                "fep.alert.queue-backlog.callback-enabled=false").run(ctx -> {
            final QueueBacklogAlertProperties p = ctx.getBean(QueueBacklogAlertProperties.class);
            assertThat(p.threshold()).isEqualTo(5000L);
            assertThat(p.callbackEnabled()).isFalse();
            assertThat(p.outboundEnabled()).isTrue();
        });
    }

    @Test
    void enabledFalse_excludesMonitorBean() {
        runner.withPropertyValues("fep.alert.queue-backlog.enabled=false").run(ctx -> {
            assertThat(ctx).hasNotFailed();
            assertThat(ctx).doesNotHaveBean(QueueBacklogMonitor.class);
        });
    }

    @Test
    void defaultMatchIfMissing_includesMonitorBean() {
        runner.run(ctx -> {
            assertThat(ctx).hasNotFailed();
            assertThat(ctx).hasSingleBean(QueueBacklogMonitor.class);
        });
    }
}
