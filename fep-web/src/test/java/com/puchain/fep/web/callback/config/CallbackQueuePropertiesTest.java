package com.puchain.fep.web.callback.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.mock.env.MockEnvironment;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CallbackQueuePropertiesTest {

    private static CallbackQueueProperties bind(final Map<String, Object> props) {
        final MockEnvironment env = new MockEnvironment();
        props.forEach(env::setProperty);
        return new Binder(ConfigurationPropertySources.get(env))
                .bindOrCreate("fep.callback", CallbackQueueProperties.class);
    }

    @Test
    void emptySource_shouldYieldDefaults() {
        final CallbackQueueProperties p = new Binder(
                ConfigurationPropertySources.get(new MockEnvironment()))
                .bindOrCreate("fep.callback", CallbackQueueProperties.class);
        assertThat(p.batchSize()).isEqualTo(50);
        assertThat(p.pollIntervalMs()).isEqualTo(5000L);
        assertThat(p.retry().backoffMillis()).isEqualTo(30000L);
        assertThat(p.retry().maxBackoffMillis()).isEqualTo(1800000L);
        assertThat(p.retry().maxAttempts()).isEqualTo(3); // PRD §5.5.2 默认 3 次
    }

    @Test
    void explicitSource_shouldOverrideDefaults() {
        final CallbackQueueProperties p = bind(Map.of(
                "fep.callback.batch-size", "20",
                "fep.callback.retry.max-attempts", "10"));
        assertThat(p.batchSize()).isEqualTo(20);
        assertThat(p.retry().maxAttempts()).isEqualTo(10);
        assertThat(p.retry().backoffMillis()).isEqualTo(30000L);
    }
}
