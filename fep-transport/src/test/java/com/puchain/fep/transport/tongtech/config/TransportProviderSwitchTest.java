package com.puchain.fep.transport.tongtech.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.puchain.fep.transport.TransportAutoConfiguration;
import com.puchain.fep.transport.mock.InMemoryDeadLetterHandler;
import com.puchain.fep.transport.mock.InMemoryMessageBroker;
import com.puchain.fep.transport.mock.InMemoryNodeLifecycleManager;
import com.puchain.fep.transport.mock.InMemoryTlqConnectionFactory;
import com.puchain.fep.transport.mock.InMemoryTlqConsumer;
import com.puchain.fep.transport.mock.InMemoryTlqProducer;

/**
 * Provider switch integration tests for {@link TransportAutoConfiguration} and
 * {@link TongtechTransportConfiguration}.
 *
 * <p>Drives Spring's {@link ApplicationContextRunner} to validate verification criteria
 * 1-3 from P1c Task 2 v1a:
 * <ul>
 *   <li>default (no provider configured) loads all mock beans;</li>
 *   <li>{@code provider=mock} loads all mock beans;</li>
 *   <li>{@code provider=tongtech} loads {@link TongtechTlqProperties} and excludes every mock bean.</li>
 * </ul></p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class TransportProviderSwitchTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withUserConfiguration(TestConfig.class);

    @Test
    @DisplayName("default provider (unset) loads mock beans via matchIfMissing=true")
    void provider_default_shouldLoadMockBeans() {
        runner.run(ctx -> {
            assertThat(ctx).hasNotFailed();
            assertThat(ctx).hasSingleBean(InMemoryTlqProducer.class);
            assertThat(ctx).hasSingleBean(InMemoryTlqConsumer.class);
            assertThat(ctx).hasSingleBean(InMemoryTlqConnectionFactory.class);
            assertThat(ctx).hasSingleBean(InMemoryNodeLifecycleManager.class);
            assertThat(ctx).hasSingleBean(InMemoryDeadLetterHandler.class);
            assertThat(ctx).hasSingleBean(InMemoryMessageBroker.class);
            assertThat(ctx).doesNotHaveBean(TongtechTlqProperties.class);
        });
    }

    @Test
    @DisplayName("provider=mock explicitly loads mock beans")
    void provider_mock_shouldLoadMockBeans() {
        runner.withPropertyValues("fep.transport.provider=mock").run(ctx -> {
            assertThat(ctx).hasNotFailed();
            assertThat(ctx).hasSingleBean(InMemoryTlqProducer.class);
            assertThat(ctx).hasSingleBean(InMemoryTlqConsumer.class);
            assertThat(ctx).hasSingleBean(InMemoryTlqConnectionFactory.class);
            assertThat(ctx).hasSingleBean(InMemoryNodeLifecycleManager.class);
            assertThat(ctx).hasSingleBean(InMemoryDeadLetterHandler.class);
            assertThat(ctx).hasSingleBean(InMemoryMessageBroker.class);
            assertThat(ctx).doesNotHaveBean(TongtechTlqProperties.class);
        });
    }

    @Test
    @DisplayName("provider=tongtech excludes every mock bean and exposes TongtechTlqProperties")
    void provider_tongtech_shouldLoadTongtechProperties() {
        runner.withPropertyValues("fep.transport.provider=tongtech").run(ctx -> {
            assertThat(ctx).hasNotFailed();
            assertThat(ctx).doesNotHaveBean(InMemoryTlqProducer.class);
            assertThat(ctx).doesNotHaveBean(InMemoryTlqConsumer.class);
            assertThat(ctx).doesNotHaveBean(InMemoryTlqConnectionFactory.class);
            assertThat(ctx).doesNotHaveBean(InMemoryNodeLifecycleManager.class);
            assertThat(ctx).doesNotHaveBean(InMemoryDeadLetterHandler.class);
            assertThat(ctx).doesNotHaveBean(InMemoryMessageBroker.class);
            // T2 only delivers Properties + Configuration skeleton; concrete Tongtech*
            // adapters arrive in T3-T7, so we only assert the configuration properties bean exists.
            assertThat(ctx).hasSingleBean(TongtechTlqProperties.class);
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
