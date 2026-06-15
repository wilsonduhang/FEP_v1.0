package com.puchain.fep.web.tracking.listener;

import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.body.supplychain.InvoCheckReturn3008;
import com.puchain.fep.processor.event.InboundMessageProcessedEvent;
import com.puchain.fep.web.tracking.service.InvoiceVerificationTrackingService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Unit behaviour for {@link InvoiceVerificationEventListener} with a mocked
 * {@link InvoiceVerificationTrackingService}.
 *
 * @author FEP Team
 * @since 1.0.0
 */
@DisplayName("InvoiceVerificationEventListener: 3008 filter + null-body skip + dispatch")
class InvoiceVerificationEventListenerTest {

    private final InvoiceVerificationTrackingService service =
            mock(InvoiceVerificationTrackingService.class);
    private final InvoiceVerificationEventListener listener =
            new InvoiceVerificationEventListener(service);

    @Test
    @DisplayName("non-3008 event → service is never invoked")
    void nonMatchingType_doesNotInvokeService() {
        listener.onProcessed(new InboundMessageProcessedEvent(
                MessageType.MSG_3116, "T0000001", "SN-X", new InvoCheckReturn3008(), Instant.now()));

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("3008 event with null body → service is never invoked")
    void nullBody_skips() {
        listener.onProcessed(new InboundMessageProcessedEvent(
                MessageType.MSG_3008, "T0000001", "SN-Y", null, Instant.now()));

        verify(service, never()).track(any(), any());
    }

    @Test
    @DisplayName("valid 3008 event → service.track invoked once with body + serialNo")
    void valid3008_invokesTrackOnce() {
        final InvoCheckReturn3008 body = new InvoCheckReturn3008();

        listener.onProcessed(new InboundMessageProcessedEvent(
                MessageType.MSG_3008, "T0000001", "SN-Z", body, Instant.now()));

        verify(service, times(1)).track(eq(body), eq("SN-Z"));
    }
}
