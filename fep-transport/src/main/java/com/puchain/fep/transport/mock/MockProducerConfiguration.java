package com.puchain.fep.transport.mock;

import com.puchain.fep.transport.TransportProperties;
import com.puchain.fep.transport.api.DeadLetterHandler;
import com.puchain.fep.transport.api.RetryableProducer;
import com.puchain.fep.transport.api.TlqProducer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Producer wiring for the {@code mock} transport provider — mirror of
 * {@link com.puchain.fep.transport.tongtech.config.TongtechProducerConfiguration}.
 *
 * <p><b>v1d design</b> (Plan §Task 5 Step 3.5, B-P1-NEW-1 fix):</p>
 * <ul>
 *   <li>{@link #inMemoryTlqProducer} — explicit {@code @Bean} factory for the
 *       underlying mock producer ({@link InMemoryTlqProducer} carries no
 *       {@code @Component} annotation, mirroring the Tongtech path so both
 *       providers expose the same wiring shape).</li>
 *   <li>{@link #retryableInMemoryProducer} — wraps the underlying mock producer
 *       with {@link RetryableProducer} and exposes it as {@link Primary} so
 *       business code injecting {@link TlqProducer} on the mock path also
 *       exercises the retry + DLH plumbing (mock parity is a deliberate
 *       requirement so dev/test environments validate the same code path).</li>
 *   <li>Tests can resolve the named bean {@code inMemoryTlqProducer} directly
 *       for raw mock-broker assertions without retry semantics.</li>
 * </ul>
 *
 * <p>Activates when {@code fep.transport.provider=mock} (the default if the
 * property is unset, via {@code matchIfMissing=true}).</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Configuration
@ConditionalOnProperty(name = "fep.transport.provider", havingValue = "mock", matchIfMissing = true)
public class MockProducerConfiguration {

    /**
     * Build the underlying in-memory mock producer (subsequently decorated by
     * {@link RetryableProducer}).
     *
     * <p>Declared as an explicit {@code @Bean} factory because
     * {@link InMemoryTlqProducer} intentionally does not carry {@code @Component}
     * (v1d sync requirement — avoids duplicate bean registration alongside the
     * {@link Primary} {@link RetryableProducer} wrapper, matching the Tongtech
     * path).</p>
     *
     * @param broker the shared in-memory broker bean (registered by
     *               {@link com.puchain.fep.transport.TransportAutoConfiguration})
     * @return the underlying mock producer
     */
    @Bean
    public InMemoryTlqProducer inMemoryTlqProducer(final InMemoryMessageBroker broker) {
        return new InMemoryTlqProducer(broker);
    }

    /**
     * Business-facing {@link Primary} {@link TlqProducer}: the underlying mock
     * producer wrapped with retry + dead-letter routing — mirrors the Tongtech
     * path so dev/test environments validate the identical retry plumbing.
     *
     * @param delegate underlying mock producer
     * @param dlh      dead-letter handler invoked when retries are exhausted
     * @param props    transport properties supplying {@code maxRetries} +
     *                 {@code retryBaseDelayMs}
     * @return retry-decorated producer marked {@link Primary}
     */
    @Bean
    @Primary
    public TlqProducer retryableInMemoryProducer(
            final InMemoryTlqProducer delegate,
            final DeadLetterHandler dlh,
            final TransportProperties props) {
        return new RetryableProducer(
                delegate, dlh,
                props.getMaxRetries(),
                props.getRetryBaseDelayMs());
    }
}
