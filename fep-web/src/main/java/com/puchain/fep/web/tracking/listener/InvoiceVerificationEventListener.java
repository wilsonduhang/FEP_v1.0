package com.puchain.fep.web.tracking.listener;

import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.body.supplychain.InvoCheckReturn3008;
import com.puchain.fep.processor.event.InboundMessageProcessedEvent;
import com.puchain.fep.web.tracking.service.InvoiceVerificationTrackingService;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Invoice verification tracking listener — wires inbound 3008 messages into
 * {@link InvoiceVerificationTrackingService#track(InvoCheckReturn3008, String)}
 * (§6.4.1 FR-DATA-DB-01).
 *
 * <p>Mirrors {@link com.puchain.fep.web.reconciliation.listener.BankReconciliationEventListener}.
 * Activated synchronously by the Spring {@code @EventListener} dispatch inside
 * {@link com.puchain.fep.web.messageinbound.service.InboundMessageDispatcher#dispatch}'s
 * {@code @Transactional} boundary (only when processing status is COMPLETED).
 * Throwing from this listener rolls back the outer dispatcher transaction,
 * keeping {@code message_process_record} and {@code invoice_verification_records}
 * consistent.</p>
 *
 * <p>Filtering: non-3008 → silent return; {@code body == null} → debug skip
 * (dispatcher still publishes when registry returns null); otherwise → track.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
@SuppressFBWarnings(value = "CRLF_INJECTION_LOGS",
        justification = "serialNo passed through LogSanitizer.sanitize() prior to LOG")
public class InvoiceVerificationEventListener {

    private static final Logger LOG =
            LoggerFactory.getLogger(InvoiceVerificationEventListener.class);

    private final InvoiceVerificationTrackingService trackingService;

    /**
     * Spring constructor injection.
     *
     * @param trackingService the invoice verification tracking service, non-null
     */
    public InvoiceVerificationEventListener(final InvoiceVerificationTrackingService trackingService) {
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
        if (event.type() != MessageType.MSG_3008) {
            return;
        }
        final InvoCheckReturn3008 body = event.bodyAs(InvoCheckReturn3008.class);
        if (body == null) {
            LOG.debug("3008 invoice tracking: body=null, skip serialNo={}",
                    LogSanitizer.sanitize(event.serialNo()));
            return;
        }
        trackingService.track(body, event.serialNo());
    }
}
