package com.puchain.fep.web.tracking.listener;

import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.body.supplychain.RzReturnInfo3009;
import com.puchain.fep.processor.event.InboundMessageProcessedEvent;
import com.puchain.fep.web.tracking.service.FinancingApplicationTrackingService;

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
 * Unit behaviour for {@link FinancingApplicationEventListener} with a mocked
 * {@link FinancingApplicationTrackingService}.
 *
 * @author FEP Team
 * @since 1.0.0
 */
@DisplayName("FinancingApplicationEventListener: 3009 filter + null-body skip + dispatch")
class FinancingApplicationEventListenerTest {

    private final FinancingApplicationTrackingService service =
            mock(FinancingApplicationTrackingService.class);
    private final FinancingApplicationEventListener listener =
            new FinancingApplicationEventListener(service);

    @Test
    @DisplayName("non-3009 event → service is never invoked")
    void nonMatchingType_doesNotInvokeService() {
        listener.onProcessed(new InboundMessageProcessedEvent(
                MessageType.MSG_3116, "T0000001", "SN-X", new RzReturnInfo3009(), Instant.now()));

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("3009 event with null body → service is never invoked")
    void nullBody_skips() {
        listener.onProcessed(new InboundMessageProcessedEvent(
                MessageType.MSG_3009, "T0000001", "SN-Y", null, Instant.now()));

        verify(service, never()).track(any(), any());
    }

    @Test
    @DisplayName("valid 3009 event → service.track invoked once with body + serialNo")
    void valid3009_invokesTrackOnce() {
        final RzReturnInfo3009 body = new RzReturnInfo3009();

        listener.onProcessed(new InboundMessageProcessedEvent(
                MessageType.MSG_3009, "T0000001", "SN-Z", body, Instant.now()));

        verify(service, times(1)).track(eq(body), eq("SN-Z"));
    }
}
