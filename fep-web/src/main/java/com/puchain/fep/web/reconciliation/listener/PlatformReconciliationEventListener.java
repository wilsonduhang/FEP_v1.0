package com.puchain.fep.web.reconciliation.listener;

import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.body.supplychain.PzCheckQuery3107;
import com.puchain.fep.processor.body.supplychain.PzCheckQueryReturn3108;
import com.puchain.fep.processor.event.InboundMessageProcessedEvent;
import com.puchain.fep.processor.reconciliation.PlatformReconciliationService;
import com.puchain.fep.processor.reconciliation.ReconciliationOutcome;
import com.puchain.fep.processor.reconciliation.ReconciliationRecord;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Platform reconciliation event listener — wires inbound 3107/3108 messages
 * into {@link PlatformReconciliationService}.
 *
 * <p>P3 Task 3 — message-driven wiring (PRD §1991 + ADR-P2e-4 Phase 2).
 * Branches by {@code event.type()}:</p>
 * <ul>
 *   <li>{@link MessageType#MSG_3107} → {@link PlatformReconciliationService#initiateOutbound}
 *       (creates a PENDING reconciliation record).</li>
 *   <li>{@link MessageType#MSG_3108} → {@link PlatformReconciliationService#processInbound}
 *       (pairs the 3107 PENDING row and emits a {@link ReconciliationOutcome}).</li>
 *   <li>any other type → silent return.</li>
 * </ul>
 *
 * <p>{@link IllegalStateException} on cast failure rolls back the dispatcher
 * transaction (registry contract violation).</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class PlatformReconciliationEventListener {

    private static final Logger LOG = LoggerFactory.getLogger(PlatformReconciliationEventListener.class);

    private final PlatformReconciliationService platformReconciliationService;

    /**
     * Spring constructor injection.
     *
     * @param platformReconciliationService the platform reconciliation service, non-null
     */
    public PlatformReconciliationEventListener(
            final PlatformReconciliationService platformReconciliationService) {
        this.platformReconciliationService = Objects.requireNonNull(
                platformReconciliationService, "platformReconciliationService");
    }

    /**
     * Synchronous {@code @EventListener} that fires inside the dispatcher's
     * {@code @Transactional} boundary.
     *
     * @param event the inbound-processed event, non-null
     * @throws IllegalStateException when the registered body POJO does not
     *                                match the declared {@code messageType}
     */
    @EventListener
    public void onProcessed(final InboundMessageProcessedEvent event) {
        final MessageType type = event.type();
        if (type != MessageType.MSG_3107 && type != MessageType.MSG_3108) {
            return;
        }
        final Object raw = event.body();
        if (raw == null) {
            LOG.debug("{} listener: body=null, skip serialNo={}",
                    type.msgNo(),
                    LogSanitizer.sanitize(event.serialNo()));
            return;
        }
        if (type == MessageType.MSG_3107) {
            if (!(raw instanceof PzCheckQuery3107 body)) {
                throw new IllegalStateException(
                        "event.body type mismatch: expected PzCheckQuery3107, got "
                                + raw.getClass().getName());
            }
            final ReconciliationRecord record =
                    platformReconciliationService.initiateOutbound(body, event.serialNo());
            LOG.info("3107 PENDING created reconciliationId={} serialNo={}",
                    record.getReconciliationId(),
                    LogSanitizer.sanitize(event.serialNo()));
        } else {
            // type == MSG_3108
            if (!(raw instanceof PzCheckQueryReturn3108 body)) {
                throw new IllegalStateException(
                        "event.body type mismatch: expected PzCheckQueryReturn3108, got "
                                + raw.getClass().getName());
            }
            final ReconciliationOutcome outcome =
                    platformReconciliationService.processInbound(body, event.serialNo());
            LOG.info("3108 paired serialNo={} status={} discrepancy={}",
                    LogSanitizer.sanitize(event.serialNo()),
                    outcome.status(),
                    outcome.discrepancyCount());
        }
    }
}
