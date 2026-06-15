package com.puchain.fep.web.tracking.listener;

import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.body.supplychain.RzReturnInfo3009;
import com.puchain.fep.processor.event.InboundMessageProcessedEvent;
import com.puchain.fep.web.tracking.service.FinancingApplicationTrackingService;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Financing application tracking listener — wires inbound 3009 messages into
 * {@link FinancingApplicationTrackingService#track(RzReturnInfo3009, String)}
 * (§6.4.1 FR-DATA-DB-01).
 *
 * <p>Mirrors {@link com.puchain.fep.web.reconciliation.listener.BankReconciliationEventListener}.
 * Fires synchronously inside the dispatcher's {@code @Transactional} boundary;
 * throwing rolls back the outer transaction, keeping {@code message_process_record}
 * and {@code financing_application_records} consistent. Non-3009 → silent return;
 * {@code body == null} → debug skip.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
@SuppressFBWarnings(value = "CRLF_INJECTION_LOGS",
        justification = "serialNo passed through LogSanitizer.sanitize() prior to LOG")
public class FinancingApplicationEventListener {

    private static final Logger LOG =
            LoggerFactory.getLogger(FinancingApplicationEventListener.class);

    private final FinancingApplicationTrackingService trackingService;

    /**
     * Spring constructor injection.
     *
     * @param trackingService the financing application tracking service, non-null
     */
    public FinancingApplicationEventListener(final FinancingApplicationTrackingService trackingService) {
        this.trackingService = Objects.requireNonNull(trackingService, "trackingService");
    }

    /**
     * Synchronous {@code @EventListener} firing inside the dispatcher's
     * {@code @Transactional} boundary.
     *
     * @param event the inbound-processed event, non-null
     */
    @EventListener
    public void onProcessed(final InboundMessageProcessedEvent event) {
        if (event.type() != MessageType.MSG_3009) {
            return;
        }
        final RzReturnInfo3009 body = event.bodyAs(RzReturnInfo3009.class);
        if (body == null) {
            LOG.debug("3009 financing tracking: body=null, skip serialNo={}",
                    LogSanitizer.sanitize(event.serialNo()));
            return;
        }
        trackingService.track(body, event.serialNo());
    }
}
