package com.puchain.fep.transport;

import com.puchain.fep.transport.mock.InMemoryMessageBroker;
import com.puchain.fep.transport.support.InMemoryMessageDeduplicator;
import com.puchain.fep.transport.support.MessageDeduplicator;
import com.puchain.fep.transport.support.QueueNameResolver;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * FEP Transport module auto-configuration.
 *
 * <p>Registers core transport beans: {@link QueueNameResolver},
 * {@link MessageDeduplicator}, and (in dev profile) {@link InMemoryMessageBroker}.
 * Mock TLQ implementations are discovered via {@link ComponentScan}.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Configuration
@EnableConfigurationProperties(TransportProperties.class)
@ComponentScan(basePackages = "com.puchain.fep.transport")
public class TransportAutoConfiguration {

    /**
     * Create a {@link QueueNameResolver} using the configured institution code.
     *
     * @param properties the transport configuration properties
     * @return a new queue name resolver instance
     */
    @Bean
    public QueueNameResolver queueNameResolver(final TransportProperties properties) {
        return new QueueNameResolver(properties.getInstitutionCode());
    }

    /**
     * Create an in-memory {@link MessageDeduplicator} with the configured capacity.
     *
     * @param properties the transport configuration properties
     * @return a new message deduplicator instance
     */
    @Bean
    public MessageDeduplicator messageDeduplicator(final TransportProperties properties) {
        return new InMemoryMessageDeduplicator(properties.getDedupCapacity());
    }

    /**
     * Create an {@link InMemoryMessageBroker} for dev/test environments.
     *
     * <p>Only active when the {@code dev} profile is enabled.</p>
     *
     * @return a new in-memory message broker
     */
    @Bean
    @Profile("dev")
    public InMemoryMessageBroker inMemoryMessageBroker() {
        return new InMemoryMessageBroker();
    }
}
