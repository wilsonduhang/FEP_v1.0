package com.puchain.fep.web.tracking.listener;

import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.body.supplychain.QyAccQueryReturn3006;
import com.puchain.fep.processor.event.InboundMessageProcessedEvent;
import com.puchain.fep.web.tracking.service.CorporateAccountTrackingService;

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
 * Unit behaviour for {@link CorporateAccountEventListener} with a mocked
 * {@link CorporateAccountTrackingService}.
 *
 * @author FEP Team
 * @since 1.0.0
 */
@DisplayName("CorporateAccountEventListener: 3006 filter + null-body skip + dispatch")
class CorporateAccountEventListenerTest {

    private final CorporateAccountTrackingService service =
            mock(CorporateAccountTrackingService.class);
    private final CorporateAccountEventListener listener =
            new CorporateAccountEventListener(service);

    @Test
    @DisplayName("non-3006 event → service is never invoked")
    void nonMatchingType_doesNotInvokeService() {
        listener.onProcessed(new InboundMessageProcessedEvent(
                MessageType.MSG_3116, "T0000001", "SN-X", new QyAccQueryReturn3006(), Instant.now()));

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("3006 event with null body → service is never invoked")
    void nullBody_skips() {
        listener.onProcessed(new InboundMessageProcessedEvent(
                MessageType.MSG_3006, "T0000001", "SN-Y", null, Instant.now()));

        verify(service, never()).track(any(), any());
    }

    @Test
    @DisplayName("valid 3006 event → service.track invoked once with body + serialNo")
    void valid3006_invokesTrackOnce() {
        final QyAccQueryReturn3006 body = new QyAccQueryReturn3006();

        listener.onProcessed(new InboundMessageProcessedEvent(
                MessageType.MSG_3006, "T0000001", "SN-Z", body, Instant.now()));

        verify(service, times(1)).track(eq(body), eq("SN-Z"));
    }
}
