package com.puchain.fep.web.reconciliation.listener;

import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.body.supplychain.BankCheckDay3116;
import com.puchain.fep.processor.body.supplychain.PzCheckQuery3107;
import com.puchain.fep.processor.body.supplychain.PzCheckQueryReturn3108;
import com.puchain.fep.processor.event.InboundMessageProcessedEvent;
import com.puchain.fep.processor.reconciliation.PlatformReconciliationService;
import com.puchain.fep.processor.reconciliation.ReconciliationOutcome;
import com.puchain.fep.processor.reconciliation.ReconciliationRecord;
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
 * Unit tests for {@link PlatformReconciliationEventListener}.
 *
 * <p>Covers 5 paths (P3 Task 3 v1a verification):</p>
 * <ol>
 *   <li>3107 + PzCheckQuery3107 body → initiateOutbound invoked once.</li>
 *   <li>3108 + PzCheckQueryReturn3108 body → processInbound invoked once.</li>
 *   <li>filter miss (3116) → both service methods untouched.</li>
 *   <li>3107 with wrong body type → throws IllegalStateException.</li>
 *   <li>null body → silent skip, service untouched.</li>
 * </ol>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class PlatformReconciliationEventListenerTest {

    private PlatformReconciliationService platformReconciliationService;
    private PlatformReconciliationEventListener listener;

    @BeforeEach
    void setUp() {
        platformReconciliationService = mock(PlatformReconciliationService.class);
        listener = new PlatformReconciliationEventListener(platformReconciliationService);
    }

    @Test
    @DisplayName("MSG_3107 with PzCheckQuery3107 body → initiateOutbound invoked once")
    void onProcessed_3107_invokesInitiateOutbound() {
        final PzCheckQuery3107 body = new PzCheckQuery3107();
        body.setSerialNo("SN20260428P107");
        final ReconciliationRecord stub = mock(ReconciliationRecord.class);
        when(stub.getReconciliationId()).thenReturn("RC_20260428_001");
        when(platformReconciliationService.initiateOutbound(eq(body), eq("SN20260428P107")))
                .thenReturn(stub);

        listener.onProcessed(new InboundMessageProcessedEvent(
                MessageType.MSG_3107, "20260428", "SN20260428P107", body, Instant.now()));

        verify(platformReconciliationService).initiateOutbound(eq(body), eq("SN20260428P107"));
        verify(platformReconciliationService, never())
                .processInbound(any(PzCheckQueryReturn3108.class), anyString());
    }

    @Test
    @DisplayName("MSG_3108 with PzCheckQueryReturn3108 body → processInbound invoked once")
    void onProcessed_3108_invokesProcessInbound() {
        final PzCheckQueryReturn3108 body = new PzCheckQueryReturn3108();
        body.setSerialNo("SN20260428P108");
        when(platformReconciliationService.processInbound(eq(body), eq("SN20260428P108")))
                .thenReturn(new ReconciliationOutcome(0, 0, ReconciliationStatus.COMPLETED, 0));

        listener.onProcessed(new InboundMessageProcessedEvent(
                MessageType.MSG_3108, "20260428", "SN20260428P108", body, Instant.now()));

        verify(platformReconciliationService).processInbound(eq(body), eq("SN20260428P108"));
        verify(platformReconciliationService, never())
                .initiateOutbound(any(PzCheckQuery3107.class), anyString());
    }

    @Test
    @DisplayName("non-platform messageType (3116) → no service interaction")
    void onProcessed_non_platform_skipsService() {
        final BankCheckDay3116 body = new BankCheckDay3116();
        body.setSerialNo("SN20260428BANK");

        listener.onProcessed(new InboundMessageProcessedEvent(
                MessageType.MSG_3116, "20260428", "SN20260428BANK", body, Instant.now()));

        verifyNoInteractions(platformReconciliationService);
    }

    @Test
    @DisplayName("MSG_3107 with wrong body type → throws IllegalStateException")
    void onProcessed_3107_typeMismatch_throws() {
        final BankCheckDay3116 wrongBody = new BankCheckDay3116();
        wrongBody.setSerialNo("SN20260428P107");

        assertThatThrownBy(() -> listener.onProcessed(new InboundMessageProcessedEvent(
                MessageType.MSG_3107, "20260428", "SN20260428P107", wrongBody, Instant.now())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("expected PzCheckQuery3107");

        verifyNoInteractions(platformReconciliationService);
    }

    @Test
    @DisplayName("null body → silent skip, service untouched")
    void onProcessed_nullBody_skipsSilently() {
        listener.onProcessed(new InboundMessageProcessedEvent(
                MessageType.MSG_3108, "20260428", "SN20260428P108", null, Instant.now()));

        verifyNoInteractions(platformReconciliationService);
    }
}
