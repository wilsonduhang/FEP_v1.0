package com.puchain.fep.web.outbound.consumer;

import com.puchain.fep.web.outbound.OutboundMessageQueueEntity;
import com.puchain.fep.web.requeststate.RequestStateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboundStatusWriterServiceTest {

    @Mock private OutboundQueueRepository repository;
    @Mock private OutboundRetryHandler retryHandler;
    @Mock private RequestStateService requestStateService;

    @InjectMocks private OutboundStatusWriterService writer;

    @Test
    void recordSent_shouldUpdateStatusAndPersist() {
        Instant sentAt = Instant.parse("2026-05-06T10:00:00Z");
        OutboundMessageQueueEntity entity = new OutboundMessageQueueEntity();
        entity.setQueueId("Q123");
        entity.setTransitionNo("00000001");
        when(repository.findById("Q123")).thenReturn(Optional.of(entity));

        writer.recordSent("Q123", "MSG-456", "TLQ-OK", sentAt);

        ArgumentCaptor<OutboundMessageQueueEntity> captor = ArgumentCaptor.forClass(OutboundMessageQueueEntity.class);
        verify(repository).save(captor.capture());
        OutboundMessageQueueEntity saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo("SENT");
        assertThat(saved.getMsgId()).isEqualTo("MSG-456");
        assertThat(saved.getSentAt()).isEqualTo(sentAt);
        assertThat(saved.getTlqSendResult()).isEqualTo("TLQ-OK");
        assertThat(saved.getUpdatedAt()).isEqualTo(sentAt);
    }

    @Test
    void recordSent_shouldMarkRequestStateSentByTransitionNo() {
        Instant sentAt = Instant.parse("2026-05-06T10:00:00Z");
        OutboundMessageQueueEntity entity = new OutboundMessageQueueEntity();
        entity.setQueueId("Q123");
        entity.setTransitionNo("00000042");
        when(repository.findById("Q123")).thenReturn(Optional.of(entity));

        writer.recordSent("Q123", "MSG-456", "TLQ-OK", sentAt);

        // SENT hook correlates by transitionNo (correlationKey), NOT queueId.
        verify(requestStateService).markSent("00000042");
    }

    @Test
    void recordSent_whenRequestStateHookThrows_shouldNotBlockSentWrite() {
        Instant sentAt = Instant.parse("2026-05-06T10:00:00Z");
        OutboundMessageQueueEntity entity = new OutboundMessageQueueEntity();
        entity.setQueueId("Q123");
        entity.setTransitionNo("00000099");
        when(repository.findById("Q123")).thenReturn(Optional.of(entity));
        when(requestStateService.markSent("00000099"))
                .thenThrow(new RuntimeException("simulated request_state failure"));

        // hook failure is isolated: recordSent must not propagate the exception
        writer.recordSent("Q123", "MSG-456", "TLQ-OK", sentAt);

        verify(repository).save(entity);
    }

    @Test
    void recordFailure_shouldNotTouchRequestStateSentHook() {
        Throwable err = new RuntimeException("boom");
        writer.recordFailure("Q789", err);
        verify(requestStateService, never()).markSent(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void recordSent_whenQueueIdNotFound_shouldThrowIllegalStateException() {
        when(repository.findById("MISSING")).thenReturn(Optional.empty());
        assertThatThrownBy(() ->
            writer.recordSent("MISSING", "MSG-X", "TLQ-OK", Instant.now()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("MISSING");
    }

    @Test
    void recordFailure_shouldDelegateToRetryHandler() {
        Throwable err = new RuntimeException("boom");
        writer.recordFailure("Q789", err);
        verify(retryHandler).handleFailure("Q789", err);
    }
}
