package com.puchain.fep.web.callback.listener;

import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.event.InboundMessageProcessedEvent;
import com.puchain.fep.web.callback.service.CallbackEnqueueService;
import com.puchain.fep.web.callback.service.CallbackTargetResolver;
import com.puchain.fep.web.submission.outputinterface.domain.SubOutputInterface;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CallbackInboundListenerTest {

    @Mock
    private CallbackTargetResolver resolver;
    @Mock
    private CallbackEnqueueService enqueueService;

    private InboundMessageProcessedEvent event(final Object body) {
        return new InboundMessageProcessedEvent(
                MessageType.MSG_2103, "T-1", "SER-9", body,
                Instant.parse("2026-05-19T06:30:00Z"));
    }

    @Test
    void onProcessed_resolveEmpty_shouldNotEnqueue() {
        when(resolver.resolve("2103")).thenReturn(List.of());
        new CallbackInboundListener(resolver, enqueueService).onProcessed(event(new Object()));
        verifyNoInteractions(enqueueService);
    }

    @Test
    void onProcessed_fanOut_shouldEnqueueOncePerInterface() {
        SubOutputInterface i1 = new SubOutputInterface();
        SubOutputInterface i2 = new SubOutputInterface();
        when(resolver.resolve("2103")).thenReturn(List.of(i1, i2));
        InboundMessageProcessedEvent ev = event(new Object());
        new CallbackInboundListener(resolver, enqueueService).onProcessed(ev);
        verify(enqueueService).enqueue(eq(i1), any(InboundMessageProcessedEvent.class));
        verify(enqueueService).enqueue(eq(i2), any(InboundMessageProcessedEvent.class));
    }
}
