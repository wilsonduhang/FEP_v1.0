package com.puchain.fep.transport.tongtech.config;

import com.puchain.fep.transport.TransportAutoConfiguration;
import com.puchain.fep.transport.api.DeadLetterHandler;
import com.puchain.fep.transport.api.RetryableProducer;
import com.puchain.fep.transport.api.TlqProducer;
import com.puchain.fep.transport.tongtech.adapter.TongtechTlqProducer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link TongtechProducerConfiguration}.
 *
 * <p>Verifies v1b acceptance criteria #7-9 (Plan §Task 5):</p>
 * <ul>
 *   <li>The {@code @Primary TlqProducer} bean is a {@link RetryableProducer} instance
 *       wrapping the underlying {@link TongtechTlqProducer}</li>
 *   <li>The named bean {@code tongtechTlqProducer} is independently injectable for
 *       low-level testing without retry semantics</li>
 *   <li>{@link TongtechTlqProducer} is registered exclusively via
 *       {@link TongtechProducerConfiguration#tongtechTlqProducer} (no {@code @Component})</li>
 * </ul>
 *
 * <p>Uses {@link ApplicationContextRunner} so {@link com.puchain.fep.transport.tongtech.adapter.TongtechTlqConnectionFactory}
 * is wired but {@code connect()} is never invoked (the factory connects lazily on
 * first send), avoiding a real broker dependency.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class TongtechProducerConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class)
            .withPropertyValues(
                    "fep.transport.provider=tongtech",
                    "fep.transport.institution-code=TEST_INST_001",
                    "fep.transport.tongtech.broker-host=127.0.0.1",
                    "fep.transport.tongtech.broker-port=10024",
                    "fep.transport.tongtech.broker-id=2",
                    "fep.transport.tongtech.qcu-name=QCU_HNDEMP_TEST_INST_1",
                    "fep.transport.tongtech.user-name=test",
                    "fep.transport.tongtech.password=test"
            );

    @Test
    @DisplayName("primary TlqProducer wraps TongtechTlqProducer with RetryableProducer")
    void tongtechProducerConfig_wrapsWithRetryableProducer() {
        runner.run(ctx -> {
            assertThat(ctx).hasNotFailed();
            final TlqProducer primary = ctx.getBean(TlqProducer.class);
            assertThat(primary).isInstanceOf(RetryableProducer.class);
            assertThat(ctx.containsBean("tongtechTlqProducer")).isTrue();
            assertThat(ctx.getBean("tongtechTlqProducer"))
                    .isInstanceOf(TongtechTlqProducer.class);
        });
    }

    @Test
    @DisplayName("fallback DeadLetterHandler is registered in tongtech path")
    void tongtechProducerConfig_registersFallbackDeadLetterHandler() {
        runner.run(ctx -> {
            assertThat(ctx).hasNotFailed();
            assertThat(ctx).hasSingleBean(DeadLetterHandler.class);
            assertThat(ctx.containsBean("tongtechFallbackDeadLetterHandler")).isTrue();
        });
    }

    /**
     * Test configuration importing both transport configurations under
     * {@code @EnableAutoConfiguration} so condition evaluation runs end-to-end.
     */
    @Configuration
    @EnableAutoConfiguration
    @Import({TransportAutoConfiguration.class, TongtechTransportConfiguration.class})
    @ComponentScan(basePackages = "com.puchain.fep.transport")
    static class TestConfig {
    }
}
