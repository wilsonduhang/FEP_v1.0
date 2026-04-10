package com.puchain.fep.transport;

import com.puchain.fep.transport.api.DeadLetterHandler;
import com.puchain.fep.transport.api.NodeLifecycleManager;
import com.puchain.fep.transport.api.TlqConnectionFactory;
import com.puchain.fep.transport.api.TlqConsumer;
import com.puchain.fep.transport.api.TlqProducer;
import com.puchain.fep.transport.mock.InMemoryDeadLetterHandler;
import com.puchain.fep.transport.mock.InMemoryNodeLifecycleManager;
import com.puchain.fep.transport.mock.InMemoryTlqConnectionFactory;
import com.puchain.fep.transport.mock.InMemoryTlqConsumer;
import com.puchain.fep.transport.mock.InMemoryTlqProducer;
import com.puchain.fep.transport.support.MessageDeduplicator;
import com.puchain.fep.transport.support.QueueNameResolver;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
@Import({InMemoryTlqProducer.class, InMemoryTlqConsumer.class,
        InMemoryTlqConnectionFactory.class, InMemoryNodeLifecycleManager.class,
        InMemoryDeadLetterHandler.class})
@ActiveProfiles("dev")
class TransportAutoConfigurationTest {

    @Autowired
    private TlqProducer producer;

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
    void allBeans_shouldBeAvailableInDevProfile() {
        assertThat(producer).isNotNull();
        assertThat(consumer).isNotNull();
        assertThat(connectionFactory).isNotNull();
        assertThat(nodeLifecycleManager).isNotNull();
        assertThat(deadLetterHandler).isNotNull();
        assertThat(messageDeduplicator).isNotNull();
        assertThat(queueNameResolver).isNotNull();
    }

    @Test
    void transportProperties_shouldHaveDefaults() {
        assertThat(properties.getRealtimePort()).isEqualTo(20001);
        assertThat(properties.getBatchPort()).isEqualTo(20002);
        assertThat(properties.getMaxRetries()).isEqualTo(3);
        assertThat(properties.getDedupCapacity()).isEqualTo(10000);
    }
}
