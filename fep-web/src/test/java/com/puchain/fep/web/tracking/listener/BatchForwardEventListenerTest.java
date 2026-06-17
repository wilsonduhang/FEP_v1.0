package com.puchain.fep.web.tracking.listener;

import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.event.BatchForwardProcessedEvent;
import com.puchain.fep.web.tracking.service.BatchForwardTrackingService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link BatchForwardEventListener} (§6.4.1 FR-DATA-DB-01):
 * delegation to the tracking service and degraded-failure swallow (DECISION-4).
 *
 * @author FEP Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class BatchForwardEventListenerTest {

    @Mock
    private BatchForwardTrackingService trackingService;

    private static BatchForwardProcessedEvent sampleEvent() {
        return new BatchForwardProcessedEvent(MessageType.MSG_3009, "T0000001",
                10, 10, 0, Instant.now(), Instant.now());
    }

    @Test
    void onProcessed_delegatesToTrackingService() {
        final BatchForwardEventListener listener = new BatchForwardEventListener(trackingService);
        final BatchForwardProcessedEvent event = sampleEvent();

        listener.onProcessed(event);

        verify(trackingService).track(event);
    }

    @Test
    void onProcessed_swallowsTrackingFailure_doesNotPropagate() {
        doThrow(new RuntimeException("db down"))
                .when(trackingService).track(any(BatchForwardProcessedEvent.class));
        final BatchForwardEventListener listener = new BatchForwardEventListener(trackingService);

        // degraded-failure semantics: side-channel audit failure must not break the main line
        assertThatCode(() -> listener.onProcessed(sampleEvent())).doesNotThrowAnyException();
    }
}
