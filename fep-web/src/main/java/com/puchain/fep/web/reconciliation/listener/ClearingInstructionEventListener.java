package com.puchain.fep.web.reconciliation.listener;

import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.body.supplychain.PlatPay3115;
import com.puchain.fep.processor.body.supplychain.QsInfo;
import com.puchain.fep.processor.event.InboundMessageProcessedEvent;
import com.puchain.fep.processor.reconciliation.ClearingInstructionRecord;
import com.puchain.fep.processor.reconciliation.ClearingInstructionService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * Clearing instruction event listener — wires inbound 3115 return messages
 * into {@link ClearingInstructionService#processInboundReturn(PlatPay3115)}.
 *
 * <p>P3 Task 3 — message-driven wiring (PRD §1991 + ADR-P2e-4 Phase 2).
 * 3115 has two operational shapes (PRD §3.4 / Plan v1a):</p>
 * <ul>
 *   <li><b>Outbound copy</b>: every {@link QsInfo#getQsReturnInfo()} is null
 *       — handled by {@code SettlementInstructionController} directly, this
 *       listener silently skips it.</li>
 *   <li><b>Inbound return</b>: at least one {@code QsReturnInfo} is non-null
 *       — this listener invokes {@link ClearingInstructionService#processInboundReturn}
 *       which updates {@code clearing_instruction_records} rows in-place.</li>
 * </ul>
 *
 * <p>{@link IllegalStateException} on cast failure rolls back the dispatcher
 * transaction (registry contract violation).</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class ClearingInstructionEventListener {

    private static final Logger LOG = LoggerFactory.getLogger(ClearingInstructionEventListener.class);

    private final ClearingInstructionService clearingInstructionService;

    /**
     * Spring constructor injection.
     *
     * @param clearingInstructionService the clearing instruction service, non-null
     */
    public ClearingInstructionEventListener(
            final ClearingInstructionService clearingInstructionService) {
        this.clearingInstructionService = Objects.requireNonNull(
                clearingInstructionService, "clearingInstructionService");
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
        if (event.type() != MessageType.MSG_3115) {
            return;
        }
        final PlatPay3115 body = event.bodyAs(PlatPay3115.class);
        if (body == null) {
            LOG.debug("3115 listener: body=null, skip serialNo={}",
                    LogSanitizer.sanitize(event.serialNo()));
            return;
        }
        final List<QsInfo> qsList = body.getQsInfo();
        final boolean isInboundReturn =
                qsList != null
                        && qsList.stream().anyMatch(qs -> qs != null && qs.getQsReturnInfo() != null);
        if (!isInboundReturn) {
            LOG.debug("3115 outbound copy (no qsReturnInfo) — skip serialNo={}",
                    LogSanitizer.sanitize(event.serialNo()));
            return;
        }
        final List<ClearingInstructionRecord> updated =
                clearingInstructionService.processInboundReturn(body);
        LOG.info("3115 inbound return processed serialNo={} updatedRows={}",
                LogSanitizer.sanitize(event.serialNo()),
                updated.size());
    }
}
