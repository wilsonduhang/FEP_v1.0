package com.puchain.fep.web.tracking.listener;

import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.body.supplychain.QyAccQueryReturn3006;
import com.puchain.fep.processor.event.InboundMessageProcessedEvent;
import com.puchain.fep.web.tracking.service.CorporateAccountTrackingService;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Corporate account tracking listener — wires inbound 3006 messages into
 * {@link CorporateAccountTrackingService#track(QyAccQueryReturn3006, String)}
 * (§6.4.1 FR-DATA-DB-01).
 *
 * <p>Mirrors {@link com.puchain.fep.web.reconciliation.listener.BankReconciliationEventListener}.
 * Fires synchronously inside the dispatcher's {@code @Transactional} boundary;
 * throwing rolls back the outer transaction. Non-3006 → silent return;
 * {@code body == null} → debug skip.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
@SuppressFBWarnings(value = "CRLF_INJECTION_LOGS",
        justification = "serialNo passed through LogSanitizer.sanitize() prior to LOG")
public class CorporateAccountEventListener {

    private static final Logger LOG =
            LoggerFactory.getLogger(CorporateAccountEventListener.class);

    private final CorporateAccountTrackingService trackingService;

    /**
     * Spring constructor injection.
     *
     * @param trackingService the corporate account tracking service, non-null
     */
    public CorporateAccountEventListener(final CorporateAccountTrackingService trackingService) {
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
        if (event.type() != MessageType.MSG_3006) {
            return;
        }
        final QyAccQueryReturn3006 body = event.bodyAs(QyAccQueryReturn3006.class);
        if (body == null) {
            LOG.debug("3006 corporate account tracking: body=null, skip serialNo={}",
                    LogSanitizer.sanitize(event.serialNo()));
            return;
        }
        trackingService.track(body, event.serialNo());
    }
}
