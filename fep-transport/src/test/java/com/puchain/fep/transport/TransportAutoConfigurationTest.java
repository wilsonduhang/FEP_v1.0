package com.puchain.fep.transport;

import com.puchain.fep.transport.api.DeadLetterHandler;
import com.puchain.fep.transport.api.NodeLifecycleManager;
import com.puchain.fep.transport.api.RetryableProducer;
import com.puchain.fep.transport.api.TlqConnectionFactory;
import com.puchain.fep.transport.api.TlqConsumer;
import com.puchain.fep.transport.api.TlqProducer;
import com.puchain.fep.transport.mock.InMemoryDeadLetterHandler;
import com.puchain.fep.transport.mock.InMemoryNodeLifecycleManager;
import com.puchain.fep.transport.mock.InMemoryTlqConnectionFactory;
import com.puchain.fep.transport.mock.InMemoryTlqConsumer;
import com.puchain.fep.transport.mock.InMemoryTlqProducer;
import com.puchain.fep.transport.mock.MockProducerConfiguration;
import com.puchain.fep.transport.support.MessageDeduplicator;
import com.puchain.fep.transport.support.QueueNameResolver;
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
 * Integration test verifying that {@link TransportAutoConfiguration} correctly
 * wires all transport beans on the default (mock) provider path.
 *
 * <p>P1c T10 efficiency E3 closing fix: migrated from {@code @SpringBootTest}
 * (~25 s full-context bootstrap) to {@link ApplicationContextRunner}
 * (~1 s per case), mirroring {@link com.puchain.fep.transport.mock.MockProducerConfigurationTest}.
 * The runner evaluates every {@code @Conditional}/{@code @Profile} the same
 * way as a real boot context, so behavioural coverage is unchanged.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class TransportAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    @Test
    @DisplayName("default provider: all beans wired to in-memory mock implementations + Primary TlqProducer = RetryableProducer")
    void allBeans_shouldBeCorrectTypesOnDefaultProvider() {
        runner.run(ctx -> {
            assertThat(ctx).hasNotFailed();
            // v1d: @Primary TlqProducer is RetryableProducer (FR-COMM-TLQ-RETRY); raw mock
            // is exposed as named bean inMemoryTlqProducer for low-level verification.
            assertThat(ctx.getBean(TlqProducer.class)).isInstanceOf(RetryableProducer.class);
            assertThat(ctx.getBean("inMemoryTlqProducer")).isInstanceOf(InMemoryTlqProducer.class);
            assertThat(ctx.getBean(TlqConsumer.class)).isInstanceOf(InMemoryTlqConsumer.class);
            assertThat(ctx.getBean(TlqConnectionFactory.class)).isInstanceOf(InMemoryTlqConnectionFactory.class);
            assertThat(ctx.getBean(NodeLifecycleManager.class)).isInstanceOf(InMemoryNodeLifecycleManager.class);
            assertThat(ctx.getBean(DeadLetterHandler.class)).isInstanceOf(InMemoryDeadLetterHandler.class);
        });
    }

    @Test
    @DisplayName("messageDeduplicator wired with default capacity (10000) from TransportProperties")
    void messageDeduplicator_shouldHaveConfiguredCapacity() {
        runner.run(ctx -> {
            assertThat(ctx).hasNotFailed();
            assertThat(ctx.getBean(MessageDeduplicator.class)).isNotNull();
            assertThat(ctx.getBean(TransportProperties.class).getDedupCapacity()).isEqualTo(10000);
        });
    }

    @Test
    @DisplayName("queueNameResolver embeds non-blank institutionCode from TransportProperties")
    void queueNameResolver_shouldResolveWithConfiguredInstitutionCode() {
        runner.run(ctx -> {
            assertThat(ctx).hasNotFailed();
            final QueueNameResolver resolver = ctx.getBean(QueueNameResolver.class);
            final String institutionCode = ctx.getBean(TransportProperties.class).getInstitutionCode();
            assertThat(institutionCode).isNotBlank();
            assertThat(resolver.resolveQcu()).contains(institutionCode);
        });
    }

    @Test
    @DisplayName("TransportProperties defaults: ports 20001/20002, maxRetries 3, dedupCapacity 10000")
    void transportProperties_shouldHaveDefaults() {
        runner.run(ctx -> {
            assertThat(ctx).hasNotFailed();
            final TransportProperties properties = ctx.getBean(TransportProperties.class);
            assertThat(properties.getRealtimePort()).isEqualTo(20001);
            assertThat(properties.getBatchPort()).isEqualTo(20002);
            assertThat(properties.getMaxRetries()).isEqualTo(3);
            assertThat(properties.getDedupCapacity()).isEqualTo(10000);
        });
    }

    /**
     * Test configuration that drives full {@code @ConditionalOnProperty}
     * evaluation through {@link EnableAutoConfiguration} and the same
     * component scan {@link TransportAutoConfiguration} declares —
     * mirrors {@link com.puchain.fep.transport.mock.MockProducerConfigurationTest.TestConfig}.
     */
    @Configuration
    @EnableAutoConfiguration
    @Import({TransportAutoConfiguration.class, TongtechTransportConfiguration.class, MockProducerConfiguration.class})
    @ComponentScan(basePackages = "com.puchain.fep.transport")
    static class TestConfig {
    }
}
