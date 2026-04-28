package com.puchain.fep.web.reconciliation.listener;

import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.body.supplychain.BankCheckDay3116;
import com.puchain.fep.processor.body.supplychain.PlatPay3115;
import com.puchain.fep.processor.body.supplychain.QsInfo;
import com.puchain.fep.processor.body.supplychain.QsReturnInfo;
import com.puchain.fep.processor.event.InboundMessageProcessedEvent;
import com.puchain.fep.processor.reconciliation.ClearingInstructionRecord;
import com.puchain.fep.processor.reconciliation.ClearingInstructionService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ClearingInstructionEventListener}.
 *
 * <p>Covers 5 paths (P3 Task 3 v1a verification — case 5 specially handles
 * the 3115 outbound copy path where every {@code QsReturnInfo} is null):</p>
 * <ol>
 *   <li>3115 + PlatPay3115 body with QsReturnInfo set → processInboundReturn invoked once.</li>
 *   <li>filter miss (3116) → service untouched.</li>
 *   <li>3115 with wrong body type → throws IllegalStateException.</li>
 *   <li>null body → silent skip, service untouched.</li>
 *   <li>3115 with all QsReturnInfo == null (outbound copy) → silent skip, service untouched.</li>
 * </ol>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class ClearingInstructionEventListenerTest {

    private ClearingInstructionService clearingInstructionService;
    private ClearingInstructionEventListener listener;

    @BeforeEach
    void setUp() {
        clearingInstructionService = mock(ClearingInstructionService.class);
        listener = new ClearingInstructionEventListener(clearingInstructionService);
    }

    @Test
    @DisplayName("3115 inbound return (qsReturnInfo non-null) → processInboundReturn invoked once")
    void onProcessed_3115_inboundReturn_invokesService() {
        final PlatPay3115 body = newPlatPayWithReturn();
        when(clearingInstructionService.processInboundReturn(eq(body)))
                .thenReturn(List.<ClearingInstructionRecord>of());

        listener.onProcessed(new InboundMessageProcessedEvent(
                MessageType.MSG_3115, "20260428", "SN20260428PAY", body, Instant.now()));

        verify(clearingInstructionService).processInboundReturn(eq(body));
    }

    @Test
    @DisplayName("non-3115 messageType (3116) → service untouched")
    void onProcessed_non3115_skipsService() {
        final BankCheckDay3116 body = new BankCheckDay3116();
        body.setSerialNo("SN20260428BANK");

        listener.onProcessed(new InboundMessageProcessedEvent(
                MessageType.MSG_3116, "20260428", "SN20260428BANK", body, Instant.now()));

        verifyNoInteractions(clearingInstructionService);
    }

    @Test
    @DisplayName("3115 with wrong body type → throws IllegalStateException")
    void onProcessed_3115_typeMismatch_throws() {
        final BankCheckDay3116 wrongBody = new BankCheckDay3116();
        wrongBody.setSerialNo("SN20260428PAY");

        assertThatThrownBy(() -> listener.onProcessed(new InboundMessageProcessedEvent(
                MessageType.MSG_3115, "20260428", "SN20260428PAY", wrongBody, Instant.now())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("expected PlatPay3115");

        verifyNoInteractions(clearingInstructionService);
    }

    @Test
    @DisplayName("null body → silent skip, service untouched")
    void onProcessed_nullBody_skipsSilently() {
        listener.onProcessed(new InboundMessageProcessedEvent(
                MessageType.MSG_3115, "20260428", "SN20260428PAY", null, Instant.now()));

        verifyNoInteractions(clearingInstructionService);
    }

    @Test
    @DisplayName("3115 outbound copy (all QsReturnInfo null) → silent skip, service untouched")
    void onProcessed_3115_outboundCopy_skipsService() {
        final PlatPay3115 body = newPlatPayWithoutReturn();

        listener.onProcessed(new InboundMessageProcessedEvent(
                MessageType.MSG_3115, "20260428", "SN20260428PAY", body, Instant.now()));

        verify(clearingInstructionService, never()).processInboundReturn(any(PlatPay3115.class));
    }

    private static PlatPay3115 newPlatPayWithReturn() {
        final PlatPay3115 body = new PlatPay3115();
        body.setSerialNo("SN20260428PAY");
        final QsInfo qs = new QsInfo();
        qs.setQsReturnInfo(new QsReturnInfo());
        final List<QsInfo> list = new ArrayList<>();
        list.add(qs);
        body.setQsInfo(list);
        return body;
    }

    private static PlatPay3115 newPlatPayWithoutReturn() {
        final PlatPay3115 body = new PlatPay3115();
        body.setSerialNo("SN20260428PAY");
        final QsInfo qs = new QsInfo();
        // qsReturnInfo intentionally left null — outbound copy
        final List<QsInfo> list = new ArrayList<>();
        list.add(qs);
        body.setQsInfo(list);
        return body;
    }
}
