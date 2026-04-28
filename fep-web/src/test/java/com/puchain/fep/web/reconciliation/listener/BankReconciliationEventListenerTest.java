package com.puchain.fep.web.reconciliation.listener;

import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.body.supplychain.BankCheckDay3116;
import com.puchain.fep.processor.body.supplychain.PzCheckQuery3107;
import com.puchain.fep.processor.event.InboundMessageProcessedEvent;
import com.puchain.fep.processor.reconciliation.BankReconciliationService;
import com.puchain.fep.processor.reconciliation.ReconciliationOutcome;
import com.puchain.fep.processor.reconciliation.ReconciliationStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link BankReconciliationEventListener}.
 *
 * <p>Covers 5 paths (P3 Task 3 v1a verification):</p>
 * <ol>
 *   <li>filter hit (3116) → service called once.</li>
 *   <li>filter miss (3107) → service untouched.</li>
 *   <li>cast failure (3116 + wrong body type) → throws IllegalStateException.</li>
 *   <li>null body → silent skip, service untouched.</li>
 *   <li>any other type → silent return.</li>
 * </ol>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class BankReconciliationEventListenerTest {

    private BankReconciliationService bankReconciliationService;
    private BankReconciliationEventListener listener;

    @BeforeEach
    void setUp() {
        bankReconciliationService = mock(BankReconciliationService.class);
        listener = new BankReconciliationEventListener(bankReconciliationService);
    }

    @Test
    @DisplayName("MSG_3116 with BankCheckDay3116 body → processInbound invoked once")
    void onProcessed_3116_invokesService() {
        final BankCheckDay3116 body = new BankCheckDay3116();
        body.setSerialNo("SN20260428BANK");
        when(bankReconciliationService.processInbound(eq(body), eq("SN20260428BANK")))
                .thenReturn(new ReconciliationOutcome(0, 0, ReconciliationStatus.COMPLETED, 0));

        listener.onProcessed(new InboundMessageProcessedEvent(
                MessageType.MSG_3116, "20260428", "SN20260428BANK", body, Instant.now()));

        verify(bankReconciliationService).processInbound(eq(body), eq("SN20260428BANK"));
    }

    @Test
    @DisplayName("non-3116 messageType (3107) → service never invoked")
    void onProcessed_non3116_skipsService() {
        final PzCheckQuery3107 body = new PzCheckQuery3107();
        body.setSerialNo("SN20260428PLAT");

        listener.onProcessed(new InboundMessageProcessedEvent(
                MessageType.MSG_3107, "20260428", "SN20260428PLAT", body, Instant.now()));

        verifyNoInteractions(bankReconciliationService);
    }

    @Test
    @DisplayName("body is wrong type → throws IllegalStateException to roll back transaction")
    void onProcessed_typeMismatch_throws() {
        final PzCheckQuery3107 wrongBody = new PzCheckQuery3107();
        wrongBody.setSerialNo("SN20260428BANK");

        assertThatThrownBy(() -> listener.onProcessed(new InboundMessageProcessedEvent(
                MessageType.MSG_3116, "20260428", "SN20260428BANK", wrongBody, Instant.now())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("expected BankCheckDay3116");

        verifyNoInteractions(bankReconciliationService);
    }

    @Test
    @DisplayName("null body → silent skip, service never invoked")
    void onProcessed_nullBody_skipsSilently() {
        listener.onProcessed(new InboundMessageProcessedEvent(
                MessageType.MSG_3116, "20260428", "SN20260428BANK", null, Instant.now()));

        verifyNoInteractions(bankReconciliationService);
    }

    @Test
    @DisplayName("MSG_3115 (other reconciliation type) → silent return")
    void onProcessed_otherType_silentReturn() {
        listener.onProcessed(new InboundMessageProcessedEvent(
                MessageType.MSG_3115, "20260428", "SN20260428PAY", null, Instant.now()));

        verify(bankReconciliationService, never()).processInbound(any(), anyString());
    }
}
