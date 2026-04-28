package com.puchain.fep.transport.mock;

import com.puchain.fep.transport.TransportAutoConfiguration;
import com.puchain.fep.transport.api.RetryableProducer;
import com.puchain.fep.transport.api.TlqProducer;
import com.puchain.fep.transport.tongtech.config.TongtechTransportConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MockProducerConfiguration}.
 *
 * <p>Verifies v1d acceptance (Plan §Task 5 Step 3.5):</p>
 * <ul>
 *   <li>The {@code @Primary TlqProducer} bean is a {@link RetryableProducer} instance
 *       wrapping the underlying {@link InMemoryTlqProducer} on the mock provider path
 *       (mirrors {@link com.puchain.fep.transport.tongtech.config.TongtechProducerConfiguration})</li>
 *   <li>The named bean {@code inMemoryTlqProducer} is independently injectable for
 *       direct mock-broker assertions in tests</li>
 *   <li>{@link InMemoryTlqProducer} is registered exclusively via
 *       {@link MockProducerConfiguration#inMemoryTlqProducer} (no {@code @Component})</li>
 * </ul>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class MockProducerConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    @Test
    @DisplayName("default provider (unset) wires InMemoryTlqProducer behind RetryableProducer")
    void mockProducerConfig_default_wrapsWithRetryableProducer() {
        runner.run(ctx -> {
            assertThat(ctx).hasNotFailed();
            final TlqProducer primary = ctx.getBean(TlqProducer.class);
            assertThat(primary).isInstanceOf(RetryableProducer.class);
            assertThat(ctx.containsBean("inMemoryTlqProducer")).isTrue();
            assertThat(ctx.getBean("inMemoryTlqProducer"))
                    .isInstanceOf(InMemoryTlqProducer.class);
        });
    }

    @Test
    @DisplayName("provider=mock explicitly wires InMemoryTlqProducer behind RetryableProducer")
    void mockProducerConfig_explicit_wrapsWithRetryableProducer() {
        runner.withPropertyValues("fep.transport.provider=mock").run(ctx -> {
            assertThat(ctx).hasNotFailed();
            final TlqProducer primary = ctx.getBean(TlqProducer.class);
            assertThat(primary).isInstanceOf(RetryableProducer.class);
            assertThat(ctx.containsBean("inMemoryTlqProducer")).isTrue();
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
