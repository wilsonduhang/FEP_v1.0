package com.puchain.fep.web.messageinbound.config;

import com.puchain.fep.transport.api.TlqConsumer;
import com.puchain.fep.transport.model.TlqChannel;
import com.puchain.fep.web.messageinbound.listener.TlqInboundListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.util.Objects;

/**
 * Subscribes {@link TlqInboundListener} to the realtime + batch receive channels.
 *
 * <p>P3 Task 3 — message-driven wiring (PRD §3.1.1 — four channels). Activated
 * only when {@code fep.transport.provider=mock} (the InMemoryTlqConsumer
 * default), aligning with the P1a Mock Mode contract until the real TLQ SDK
 * arrives.</p>
 *
 * <p>The two receive channels share port semantics with the upstream HNDEMP
 * endpoints (20001 realtime, 20002 batch). Both deliver into the same
 * {@code TlqInboundListener} which routes via the {@code InboundMessageDispatcher}.</p>
 *
 * <p>Activation is a {@code @PostConstruct} side-effect rather than a {@code @Bean}
 * factory because the {@link InMemoryTlqConsumer} is already a {@code @Component}
 * (registered by the transport module) — re-declaring it here would conflict
 * with the existing bean. We only register the subscription wiring.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Configuration
@ConditionalOnProperty(
        name = "fep.transport.provider",
        havingValue = "mock",
        matchIfMissing = true)
public class TlqInboundConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(TlqInboundConfiguration.class);

    private final TlqConsumer tlqConsumer;
    private final TlqInboundListener tlqInboundListener;

    /**
     * Spring constructor injection.
     *
     * @param tlqConsumer        the TLQ consumer (mock or real), non-null
     * @param tlqInboundListener the inbound listener, non-null
     */
    public TlqInboundConfiguration(final TlqConsumer tlqConsumer,
                                    final TlqInboundListener tlqInboundListener) {
        this.tlqConsumer = Objects.requireNonNull(tlqConsumer, "tlqConsumer");
        this.tlqInboundListener = Objects.requireNonNull(tlqInboundListener, "tlqInboundListener");
    }

    /**
     * Subscribe to both inbound channels at application startup.
     */
    @PostConstruct
    public void registerSubscriptions() {
        tlqConsumer.subscribe(TlqChannel.REALTIME_RECEIVE, tlqInboundListener);
        tlqConsumer.subscribe(TlqChannel.BATCH_RECEIVE, tlqInboundListener);
        LOG.info("TLQ inbound subscriptions registered: REALTIME_RECEIVE + BATCH_RECEIVE");
    }
}
