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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test verifying that {@link TransportAutoConfiguration}
 * correctly wires all transport beans in the dev profile.
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest(classes = TransportAutoConfiguration.class)
@Import({InMemoryTlqConsumer.class, InMemoryTlqConnectionFactory.class,
        InMemoryNodeLifecycleManager.class, InMemoryDeadLetterHandler.class,
        MockProducerConfiguration.class})
@ActiveProfiles("dev")
class TransportAutoConfigurationTest {

    @Autowired
    private TlqProducer producer;

    @Autowired
    @Qualifier("inMemoryTlqProducer")
    private InMemoryTlqProducer rawInMemoryProducer;

    @Autowired
    private TlqConsumer consumer;

    @Autowired
    private TlqConnectionFactory connectionFactory;

    @Autowired
    private NodeLifecycleManager nodeLifecycleManager;

    @Autowired
    private DeadLetterHandler deadLetterHandler;

    @Autowired
    private MessageDeduplicator messageDeduplicator;

    @Autowired
    private QueueNameResolver queueNameResolver;

    @Autowired
    private TransportProperties properties;

    @Test
    void allBeans_shouldBeCorrectTypesInDevProfile() {
        // v1d: @Primary TlqProducer is RetryableProducer (FR-COMM-TLQ-RETRY); raw mock
        // is exposed as named bean inMemoryTlqProducer for low-level verification.
        assertThat(producer).isInstanceOf(RetryableProducer.class);
        assertThat(rawInMemoryProducer).isInstanceOf(InMemoryTlqProducer.class);
        assertThat(consumer).isInstanceOf(InMemoryTlqConsumer.class);
        assertThat(connectionFactory).isInstanceOf(InMemoryTlqConnectionFactory.class);
        assertThat(nodeLifecycleManager).isInstanceOf(InMemoryNodeLifecycleManager.class);
        assertThat(deadLetterHandler).isInstanceOf(InMemoryDeadLetterHandler.class);
    }

    @Test
    void messageDeduplicator_shouldHaveConfiguredCapacity() {
        assertThat(messageDeduplicator).isNotNull();
        // dev profile uses default dedupCapacity=10000 — verifies binding wired correctly
        assertThat(properties.getDedupCapacity()).isEqualTo(10000);
    }

    @Test
    void queueNameResolver_shouldResolveWithConfiguredInstitutionCode() {
        assertThat(queueNameResolver).isNotNull();
        // resolver must use the institution code from properties, not an empty/null value
        final String institutionCode = properties.getInstitutionCode();
        assertThat(institutionCode).isNotBlank();
        assertThat(queueNameResolver.resolveQcu()).contains(institutionCode);
    }

    @Test
    void transportProperties_shouldHaveDefaults() {
        assertThat(properties.getRealtimePort()).isEqualTo(20001);
        assertThat(properties.getBatchPort()).isEqualTo(20002);
        assertThat(properties.getMaxRetries()).isEqualTo(3);
        assertThat(properties.getDedupCapacity()).isEqualTo(10000);
    }
}
