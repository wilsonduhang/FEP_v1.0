package com.puchain.fep.web.reconciliation.listener;

import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.body.supplychain.BankCheckDay3116;
import com.puchain.fep.processor.event.InboundMessageProcessedEvent;
import com.puchain.fep.processor.reconciliation.BankReconciliationService;
import com.puchain.fep.processor.reconciliation.ReconciliationOutcome;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Bank reconciliation event listener — wires inbound 3116 messages into
 * {@link BankReconciliationService#processInbound(BankCheckDay3116, String)}.
 *
 * <p>P3 Task 3 — message-driven wiring (PRD §1991 + ADR-P2e-4 Phase 2).
 * Activated synchronously by the Spring {@code @EventListener} dispatch
 * inside {@link com.puchain.fep.web.messageinbound.service.InboundMessageDispatcher#dispatch}'s
 * {@code @Transactional} boundary. Throwing from this listener rolls back the
 * outer dispatcher transaction, which keeps {@code message_process_record}
 * and {@code reconciliation_records} consistent.</p>
 *
 * <p>Filtering rules:</p>
 * <ul>
 *   <li>{@code event.type() != MSG_3116} → silent return (no log; other listeners may match).</li>
 *   <li>{@code event.body() == null} → debug log + silent skip (the dispatcher
 *       still publishes events when registry returns {@code null}; defensive
 *       check protects this listener from NPE).</li>
 *   <li>{@code body} type mismatch → throw {@link IllegalStateException}
 *       to force rollback (registry contract violation).</li>
 *   <li>otherwise → invoke {@code service.processInbound(body, serialNo)}.</li>
 * </ul>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class BankReconciliationEventListener {

    private static final Logger LOG = LoggerFactory.getLogger(BankReconciliationEventListener.class);

    private final BankReconciliationService bankReconciliationService;

    /**
     * Spring constructor injection.
     *
     * @param bankReconciliationService the bank reconciliation service, non-null
     */
    public BankReconciliationEventListener(final BankReconciliationService bankReconciliationService) {
        this.bankReconciliationService = Objects.requireNonNull(
                bankReconciliationService, "bankReconciliationService");
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
        if (event.type() != MessageType.MSG_3116) {
            return;
        }
        final Object raw = event.body();
        if (raw == null) {
            LOG.debug("3116 listener: body=null, skip serialNo={}",
                    LogSanitizer.sanitize(event.serialNo()));
            return;
        }
        if (!(raw instanceof BankCheckDay3116 body)) {
            throw new IllegalStateException(
                    "event.body type mismatch: expected BankCheckDay3116, got "
                            + raw.getClass().getName());
        }
        final ReconciliationOutcome outcome =
                bankReconciliationService.processInbound(body, event.serialNo());
        LOG.info("3116 reconciled serialNo={} status={} discrepancy={}",
                LogSanitizer.sanitize(event.serialNo()),
                outcome.status(),
                outcome.discrepancyCount());
    }
}
