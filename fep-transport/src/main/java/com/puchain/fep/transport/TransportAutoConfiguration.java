package com.puchain.fep.transport;

import com.puchain.fep.transport.mock.InMemoryMessageBroker;
import com.puchain.fep.transport.support.InMemoryMessageDeduplicator;
import com.puchain.fep.transport.support.MessageDeduplicator;
import com.puchain.fep.transport.support.QueueNameResolver;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * FEP Transport module auto-configuration.
 *
 * <p>Registers core transport beans: {@link QueueNameResolver},
 * {@link MessageDeduplicator}, and (when {@code fep.transport.provider=mock})
 * {@link InMemoryMessageBroker}. Mock TLQ implementations are discovered via
 * {@link ComponentScan} and gated individually by
 * {@link ConditionalOnProperty @ConditionalOnProperty}.</p>
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
     * Create an {@link InMemoryMessageBroker} for the {@code mock} transport provider.
     *
     * <p>Only active when {@code fep.transport.provider=mock} (the default when the property
     * is not set, via {@code matchIfMissing=true}).</p>
     *
     * @return a new in-memory message broker
     */
    @Bean
    @ConditionalOnProperty(
        name = "fep.transport.provider",
        havingValue = "mock",
        matchIfMissing = true
    )
    public InMemoryMessageBroker inMemoryMessageBroker() {
        return new InMemoryMessageBroker();
    }
}
