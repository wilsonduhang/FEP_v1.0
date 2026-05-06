package com.puchain.fep.web.outbound.consumer;

import com.puchain.fep.web.outbound.OutboundMessageQueueEntity;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboundStatusWriterTest {

    @Mock private OutboundQueueRepository repository;
    @Mock private OutboundRetryHandler retryHandler;

    @InjectMocks private OutboundStatusWriter writer;

    @Test
    void recordSent_shouldUpdateStatusAndPersist() {
        Instant sentAt = Instant.parse("2026-05-06T10:00:00Z");
        OutboundMessageQueueEntity entity = new OutboundMessageQueueEntity();
        entity.setQueueId("Q123");
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
