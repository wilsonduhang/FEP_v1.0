package com.puchain.fep.transport.tongtech.config;

import com.puchain.fep.transport.TransportProperties;
import com.puchain.fep.transport.api.DeadLetterHandler;
import com.puchain.fep.transport.api.RetryableProducer;
import com.puchain.fep.transport.api.TlqProducer;
import com.puchain.fep.transport.support.QueueNameResolver;
import com.puchain.fep.transport.tongtech.adapter.TongtechChannelMapper;
import com.puchain.fep.transport.tongtech.adapter.TongtechMessageMapper;
import com.puchain.fep.transport.tongtech.adapter.TongtechTlqConnectionFactory;
import com.puchain.fep.transport.tongtech.adapter.TongtechTlqProducer;
import com.puchain.fep.transport.tongtech.error.TongtechErrorMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Producer wiring for the Tongtech (real SDK) transport provider.
 *
 * <p><b>v1b design</b> (Plan §Task 5 B-P1-2):</p>
 * <ul>
 *   <li>{@link #tongtechTlqProducer} — explicit {@code @Bean} factory for the
 *       underlying SDK producer ({@link TongtechTlqProducer} carries no
 *       {@code @Component} annotation, avoiding double registration alongside
 *       the {@link RetryableProducer} wrapper).</li>
 *   <li>{@link #retryableTongtechProducer} — wraps the underlying producer with
 *       {@link RetryableProducer} and exposes it as {@link Primary} so business
 *       code injecting {@link TlqProducer} automatically gains retry + DLH
 *       routing (FR-COMM-TLQ-RETRY).</li>
 *   <li>Tests can resolve the named bean {@code tongtechTlqProducer} directly to
 *       assert raw SDK behaviour without retry semantics.</li>
 * </ul>
 *
 * <p>Activates only when {@code fep.transport.provider=tongtech}.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Configuration
@ConditionalOnProperty(name = "fep.transport.provider", havingValue = "tongtech")
public class TongtechProducerConfiguration {

    private static final Logger DLH_LOG = LoggerFactory.getLogger(
            "com.puchain.fep.transport.tongtech.dlh.fallback");

    /**
     * Fallback {@link DeadLetterHandler} for the Tongtech transport path.
     *
     * <p><b>Plan gap fix (T5 closing addendum)</b>: Plan v1d acceptance #7 requires
     * {@link RetryableProducer} wrapping with a {@link DeadLetterHandler} dependency,
     * but the v1d §Files clause did not list a tongtech-side DLH source — the existing
     * {@link com.puchain.fep.transport.mock.InMemoryDeadLetterHandler} is gated to
     * {@code provider=mock}. This {@code @Bean} provides a log-only fallback so the
     * Spring context starts and {@link RetryableProducer} has a target on retry
     * exhaustion. Marked {@link ConditionalOnMissingBean} so a future task (T7 or a
     * standalone ticket) can swap in a real DLQ-publishing handler without touching
     * this configuration.</p>
     *
     * <p>Behaviour: ERROR-level log only (DLH is the terminal failure path); does not
     * retain messages in memory (avoids unbounded heap growth in production).</p>
     *
     * <p><b>Follow-up</b>: Real DLQ topic publication is deferred per Plan §Risk R6
     * (DLQ true-machine calibration → T7 / standalone ticket).</p>
     *
     * @return log-only fallback dead-letter handler
     */
    @Bean
    @ConditionalOnMissingBean
    public DeadLetterHandler tongtechFallbackDeadLetterHandler() {
        return (message, reason) -> DLH_LOG.error(
                "Dead letter routed (tongtech fallback): msgId={} reason={}",
                message.getMsgId(), reason);
    }

    /**
     * Build the underlying Tongtech SDK producer (subsequently decorated by
     * {@link RetryableProducer}).
     *
     * <p>Declared as an explicit {@code @Bean} factory because
     * {@link TongtechTlqProducer} intentionally does not carry {@code @Component}
     * (Plan §Task 5 v1b acceptance #9 — avoids duplicate bean registration with
     * the {@code @Primary} {@link RetryableProducer} wrapper).</p>
     *
     * @param factory       SDK connection factory
     * @param mapper        FEP ↔ SDK message mapper
     * @param resolver      queue name resolver
     * @param channelMapper FEP channel → queue type mapper
     * @param errorMapper   SDK exception → FEP error code mapper
     * @return the underlying SDK producer
     */
    @Bean
    public TongtechTlqProducer tongtechTlqProducer(
            final TongtechTlqConnectionFactory factory,
            final TongtechMessageMapper mapper,
            final QueueNameResolver resolver,
            final TongtechChannelMapper channelMapper,
            final TongtechErrorMapper errorMapper) {
        return new TongtechTlqProducer(factory, mapper, resolver, channelMapper, errorMapper);
    }

    /**
     * Business-facing {@link Primary} {@link TlqProducer}: the underlying SDK
     * producer wrapped with retry + dead-letter routing.
     *
     * <p>Depends on {@link TongtechTlqProducer#send} returning {@link com.puchain.fep.transport.api.SendResult}
     * (no throw on send-level failures, see v1b B-P0-1) so {@link RetryableProducer}
     * can observe the failure and drive retries.</p>
     *
     * @param delegate underlying Tongtech SDK producer
     * @param dlh      dead-letter handler invoked when retries are exhausted
     * @param props    transport properties supplying {@code maxRetries} +
     *                 {@code retryBaseDelayMs}
     * @return retry-decorated producer marked {@link Primary}
     */
    @Bean
    @Primary
    public TlqProducer retryableTongtechProducer(
            final TongtechTlqProducer delegate,
            final DeadLetterHandler dlh,
            final TransportProperties props) {
        return new RetryableProducer(
                delegate, dlh,
                props.getMaxRetries(),
                props.getRetryBaseDelayMs());
    }
}
