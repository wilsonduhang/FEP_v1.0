package com.puchain.fep.web.outbound.consumer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link OutboundQueueProperties} 绑定单元测试。
 *
 * <p>验证 Spring Boot {@code @ConfigurationProperties} relaxed binding 能正确
 * 把 kebab-case 配置项映射到嵌套 POJO 字段（包含 retry 子对象）。</p>
 */
class OutboundQueuePropertiesTest {

    @Test
    void shouldBindProperties() {
        var src = new MapConfigurationPropertySource(Map.of(
                "fep.outbound.queue.batch-size", "50",
                "fep.outbound.queue.poll-interval-ms", "1000",
                "fep.outbound.queue.retry.backoff-millis", "30000",
                "fep.outbound.queue.retry.max-backoff-millis", "1800000",
                "fep.outbound.queue.retry.max-attempts", "5"
        ));
        OutboundQueueProperties p = new Binder(src)
                .bind("fep.outbound.queue", OutboundQueueProperties.class)
                .get();
        assertThat(p.getBatchSize()).isEqualTo(50);
        assertThat(p.getPollIntervalMs()).isEqualTo(1000L);
        assertThat(p.getRetry().getBackoffMillis()).isEqualTo(30_000L);
        assertThat(p.getRetry().getMaxBackoffMillis()).isEqualTo(1_800_000L);
        assertThat(p.getRetry().getMaxAttempts()).isEqualTo(5);
    }
}
