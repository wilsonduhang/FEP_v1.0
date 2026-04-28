package com.puchain.fep.transport.tongtech.config;

import com.puchain.fep.transport.TransportAutoConfiguration;
import com.puchain.fep.transport.api.RemoteAdmin;
import com.puchain.fep.transport.mock.InMemoryRemoteAdmin;
import com.puchain.fep.transport.tongtech.adapter.TongtechRemoteAdmin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Assembly tests for the {@link RemoteAdmin} interface — verifies that exactly
 * one {@link RemoteAdmin} bean is registered per {@code fep.transport.provider}
 * value (mock default / mock explicit / tongtech).
 *
 * <p>Mirrors {@link TransportProviderSwitchTest}'s pattern (P1c T2 v1a).</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class RemoteAdminAssemblyTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    @Test
    @DisplayName("default provider 装配 InMemoryRemoteAdmin（matchIfMissing=true）")
    void defaultProvider_shouldLoadInMemoryRemoteAdmin() {
        runner.run(ctx -> {
            assertThat(ctx).hasNotFailed();
            assertThat(ctx).hasSingleBean(RemoteAdmin.class);
            assertThat(ctx).hasSingleBean(InMemoryRemoteAdmin.class);
            assertThat(ctx).doesNotHaveBean(TongtechRemoteAdmin.class);
        });
    }

    @Test
    @DisplayName("provider=mock 显式装配 InMemoryRemoteAdmin")
    void providerMock_shouldLoadInMemoryRemoteAdmin() {
        runner.withPropertyValues("fep.transport.provider=mock").run(ctx -> {
            assertThat(ctx).hasNotFailed();
            assertThat(ctx).hasSingleBean(RemoteAdmin.class);
            assertThat(ctx).hasSingleBean(InMemoryRemoteAdmin.class);
            assertThat(ctx).doesNotHaveBean(TongtechRemoteAdmin.class);
        });
    }

    @Test
    @DisplayName("provider=tongtech 装配 TongtechRemoteAdmin（替代 InMemory）")
    void providerTongtech_shouldLoadTongtechRemoteAdmin() {
        runner.withPropertyValues("fep.transport.provider=tongtech").run(ctx -> {
            assertThat(ctx).hasNotFailed();
            assertThat(ctx).hasSingleBean(RemoteAdmin.class);
            assertThat(ctx).hasSingleBean(TongtechRemoteAdmin.class);
            assertThat(ctx).doesNotHaveBean(InMemoryRemoteAdmin.class);
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
