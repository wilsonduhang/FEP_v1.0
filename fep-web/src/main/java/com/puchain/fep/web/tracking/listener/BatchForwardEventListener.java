package com.puchain.fep.web.tracking.listener;

import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.processor.event.BatchForwardProcessedEvent;
import com.puchain.fep.web.tracking.service.BatchForwardTrackingService;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Batch forward tracking listener — wires {@link BatchForwardProcessedEvent}
 * (published by the {@code fep-processor} batch pipeline) into
 * {@link BatchForwardTrackingService#track(BatchForwardProcessedEvent)}
 * (§6.4.1 FR-DATA-DB-01).
 *
 * <p>Degraded-failure semantics (DECISION-4): unlike the inbound-dispatcher
 * listeners (which throw to roll back the dispatcher transaction), the batch
 * forward record is a side-channel audit. The event is published from
 * {@code fep-processor} with no surrounding web transaction, and a tracking
 * failure must NOT roll back / break the batch processing main line. Therefore
 * this listener swallows any {@link RuntimeException} with a WARN.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
@SuppressFBWarnings(value = "CRLF_INJECTION_LOGS",
        justification = "transitionNo passed through LogSanitizer.sanitize() prior to LOG")
public class BatchForwardEventListener {

    private static final Logger LOG =
            LoggerFactory.getLogger(BatchForwardEventListener.class);

    private final BatchForwardTrackingService trackingService;

    /**
     * Spring constructor injection.
     *
     * @param trackingService the batch forward tracking service, non-null
     */
    public BatchForwardEventListener(final BatchForwardTrackingService trackingService) {
        this.trackingService = Objects.requireNonNull(trackingService, "trackingService");
    }

    /**
     * Synchronous {@code @EventListener} firing in the batch pipeline's publishing
     * thread. Persists the batch forward record; swallows tracking failures so the
     * batch main line is never disrupted by side-channel audit errors.
     *
     * @param event the batch forward processed event, non-null
     */
    @EventListener
    public void onProcessed(final BatchForwardProcessedEvent event) {
        try {
            trackingService.track(event);
        } catch (final RuntimeException ex) {
            LOG.warn("batch forward tracking failed (swallowed) serialNo={}",
                    LogSanitizer.sanitize(event.transitionNo()), ex);
        }
    }
}
