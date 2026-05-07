package com.puchain.fep.web.outbound.consumer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link OutboundQueueProperties} 绑定与不可变性单元测试。
 *
 * <p>验证 Spring Boot {@code @ConfigurationProperties} relaxed binding 能正确
 * 把 kebab-case 配置项映射到 record components（包含 retry 子 record），
 * 同时校验 record 不暴露 setter（B1 #2-#4 EI_EXPOSE_REP/REP2 永久修法）。</p>
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
        assertThat(p.batchSize()).isEqualTo(50);
        assertThat(p.pollIntervalMs()).isEqualTo(1000L);
        assertThat(p.retry().backoffMillis()).isEqualTo(30_000L);
        assertThat(p.retry().maxBackoffMillis()).isEqualTo(1_800_000L);
        assertThat(p.retry().maxAttempts()).isEqualTo(5);
    }

    @Test
    void properties_shouldBeImmutableRecord() {
        // record 类型 + 无 setter 方法 = immutable（消除 EI_EXPOSE_REP/REP2）
        assertThat(OutboundQueueProperties.class.isRecord()).isTrue();
        assertThat(OutboundQueueProperties.Retry.class.isRecord()).isTrue();
        List<String> setters = Arrays.stream(OutboundQueueProperties.class.getMethods())
                .map(java.lang.reflect.Method::getName)
                .filter(n -> n.startsWith("set"))
                .toList();
        assertThat(setters).isEmpty();
        List<String> retrySetters = Arrays.stream(OutboundQueueProperties.Retry.class.getMethods())
                .map(java.lang.reflect.Method::getName)
                .filter(n -> n.startsWith("set"))
                .toList();
        assertThat(retrySetters).isEmpty();
    }

    @Test
    void properties_shouldExposeRecordAccessors() {
        var props = new OutboundQueueProperties(100, 2000L,
                new OutboundQueueProperties.Retry(60_000L, 1_800_000L, 3));
        assertThat(props.batchSize()).isEqualTo(100);
        assertThat(props.pollIntervalMs()).isEqualTo(2000L);
        assertThat(props.retry().backoffMillis()).isEqualTo(60_000L);
        assertThat(props.retry().maxBackoffMillis()).isEqualTo(1_800_000L);
        assertThat(props.retry().maxAttempts()).isEqualTo(3);
    }
}
