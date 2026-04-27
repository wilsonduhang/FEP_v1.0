package com.puchain.fep.transport.tongtech.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Tongtech (real TLQ SDK) transport configuration.
 *
 * <p>Activates when {@code fep.transport.provider=tongtech}. Enables
 * {@link TongtechTlqProperties} binding and component scans the
 * {@code com.puchain.fep.transport.tongtech} package so the production
 * {@code Tongtech*} adapters (added in P1c Tasks 3-7) are discovered.</p>
 *
 * <p>Task 2 only delivers the configuration skeleton + {@link TongtechTlqProperties}.
 * The concrete {@code Tongtech*} {@code @Component} adapters (TlqProducer, TlqConsumer,
 * TlqConnectionFactory, NodeLifecycleManager, DeadLetterHandler) land in subsequent
 * tasks.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Configuration
@ConditionalOnProperty(name = "fep.transport.provider", havingValue = "tongtech")
@EnableConfigurationProperties(TongtechTlqProperties.class)
@ComponentScan(basePackages = "com.puchain.fep.transport.tongtech")
public class TongtechTransportConfiguration {
}
