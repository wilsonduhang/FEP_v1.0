package com.puchain.fep.web.outbound.consumer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link OutboundQueueProperties} 默认值绑定测试（B1 #2-#4 永久修法）。
 *
 * <p>验证当配置源完全空白时，{@code @DefaultValue} 注解能为顶层 record components
 * 与嵌套 {@link OutboundQueueProperties.Retry} record 提供默认值（Spring Boot 3.1+ 支持）。
 * 双保险：application.yml 同时声明显式默认值，本测试使用空配置源以单独校验
 * {@code @DefaultValue} 注解层的兜底语义（不依赖 yml 加载）。</p>
 *
 * <p>v1.1 修订（评审 W1）：拆顶层独立 class，避免 JUnit 5 嵌套
 * {@code @SpringBootTest} 行为依赖 Spring Test 版本 + Surefire 扫描盲区。</p>
 */
class OutboundQueuePropertiesDefaultsTest {

    @Test
    void shouldUseDefaultsWhenNoPropertiesSet() {
        var src = new MapConfigurationPropertySource(Map.of());
        OutboundQueueProperties props = new Binder(src)
                .bindOrCreate("fep.outbound.queue", OutboundQueueProperties.class);
        assertThat(props.batchSize()).isEqualTo(50);
        assertThat(props.pollIntervalMs()).isEqualTo(1000L);
        assertThat(props.retry()).isNotNull();
        assertThat(props.retry().backoffMillis()).isEqualTo(30_000L);
        assertThat(props.retry().maxBackoffMillis()).isEqualTo(1_800_000L);
        assertThat(props.retry().maxAttempts()).isEqualTo(5);
    }

    @Test
    void shouldApplyTopLevelDefaultsAndExplicitRetry() {
        // 顶层 batchSize / pollIntervalMs 默认 + retry 用户显式指定
        var src = new MapConfigurationPropertySource(Map.of(
                "fep.outbound.queue.retry.backoff-millis", "10000",
                "fep.outbound.queue.retry.max-backoff-millis", "60000",
                "fep.outbound.queue.retry.max-attempts", "3"
        ));
        OutboundQueueProperties props = new Binder(src)
                .bindOrCreate("fep.outbound.queue", OutboundQueueProperties.class);
        assertThat(props.batchSize()).isEqualTo(50);
        assertThat(props.pollIntervalMs()).isEqualTo(1000L);
        assertThat(props.retry().backoffMillis()).isEqualTo(10_000L);
        assertThat(props.retry().maxBackoffMillis()).isEqualTo(60_000L);
        assertThat(props.retry().maxAttempts()).isEqualTo(3);
    }
}
