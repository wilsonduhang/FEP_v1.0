package com.puchain.fep.web.callback.service;

import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.event.InboundMessageProcessedEvent;
import com.puchain.fep.web.callback.domain.CallbackQueueEntity;
import com.puchain.fep.web.callback.repository.CallbackQueueRepository;
import com.puchain.fep.web.submission.outputinterface.domain.SubOutputInterface;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CallbackEnqueueServiceTest {

    @Mock
    private CallbackQueueRepository repository;
    @Mock
    private CallbackEnvelopeBuilder envelopeBuilder;

    private InboundMessageProcessedEvent event() {
        return new InboundMessageProcessedEvent(
                MessageType.MSG_2103, "T-1", "SER-9", new Object(),
                Instant.parse("2026-05-19T06:30:00Z"));
    }

    @Test
    void enqueue_shouldBeAnnotatedRequiresNew() throws Exception {
        Method m = CallbackEnqueueService.class.getMethod(
                "enqueue", SubOutputInterface.class, InboundMessageProcessedEvent.class);
        Transactional tx = m.getAnnotation(Transactional.class);
        assertThat(tx).isNotNull();
        assertThat(tx.propagation()).isEqualTo(Propagation.REQUIRES_NEW);
    }

    @Test
    void enqueue_duplicateIdempotencyKey_shouldSkip() {
        SubOutputInterface target = new SubOutputInterface();
        target.setInterfaceId("if-1");
        when(repository.existsByIdempotencyKey(any())).thenReturn(true);
        new CallbackEnqueueService(repository, envelopeBuilder).enqueue(target, event());
        verify(repository, never()).save(any());
    }

    @Test
    void enqueue_newKey_shouldSaveEntityWithDerivedKeyAndInterface() {
        SubOutputInterface target = new SubOutputInterface();
        target.setInterfaceId("if-1");
        when(repository.existsByIdempotencyKey(any())).thenReturn(false);
        when(envelopeBuilder.build(any())).thenReturn("{\"code\":\"200\"}");

        new CallbackEnqueueService(repository, envelopeBuilder).enqueue(target, event());

        ArgumentCaptor<CallbackQueueEntity> captor =
                ArgumentCaptor.forClass(CallbackQueueEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getIdempotencyKey()).hasSize(32);
        assertThat(captor.getValue().getTargetInterfaceId()).isEqualTo("if-1");
        assertThat(captor.getValue().getMsgNo()).isEqualTo("2103");
        assertThat(captor.getValue().getPayloadJson()).isEqualTo("{\"code\":\"200\"}");
    }
}
