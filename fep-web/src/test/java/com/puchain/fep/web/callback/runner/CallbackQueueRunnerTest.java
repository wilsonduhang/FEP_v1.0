package com.puchain.fep.web.callback.runner;

import com.puchain.fep.web.callback.config.CallbackQueueProperties;
import com.puchain.fep.web.callback.domain.CallbackQueueEntity;
import com.puchain.fep.web.callback.domain.CallbackQueueStatus;
import com.puchain.fep.web.callback.http.CallbackHttpClient;
import com.puchain.fep.web.callback.http.CallbackResult;
import com.puchain.fep.web.callback.metrics.CallbackMetrics;
import com.puchain.fep.web.callback.repository.CallbackQueueRepository;
import com.puchain.fep.web.submission.outputinterface.domain.InterfaceAuthType;
import com.puchain.fep.web.submission.outputinterface.domain.SubOutputInterface;
import com.puchain.fep.web.submission.outputinterface.repository.SubOutputInterfaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link CallbackQueueRunner} 单元测试 — Mockito 驱动，验证 Phase 2 行为：
 * claimBatch 声领、markSending 先于 http、DONE writeback、retryHandler 委托、
 * interface not found FAILED、per-row 异常隔离。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class CallbackQueueRunnerTest {

    @Mock
    private CallbackQueueRepository callbackQueueRepository;

    @Mock
    private CallbackHttpClient httpClient;

    @Mock
    private SubOutputInterfaceRepository subOutputInterfaceRepository;

    @Mock
    private CallbackRetryHandler retryHandler;

    @Mock
    private CallbackMetrics metrics;

    private CallbackQueueProperties props;
    private CallbackQueueRunner runner;

    @BeforeEach
    void setUp() {
        props = new CallbackQueueProperties(50, 5000L,
                new CallbackQueueProperties.Retry(30000L, 1800000L, 3),
                new CallbackQueueProperties.Reaper(true, 60000L, 300L));
        runner = new CallbackQueueRunner(callbackQueueRepository, httpClient,
                subOutputInterfaceRepository, props, retryHandler, metrics);
    }

    private CallbackQueueEntity pendingEntity(final String queueId, final String interfaceId) {
        return CallbackQueueEntity.pending(
                "idem-" + queueId, interfaceId, "2101", "{\"test\":true}");
    }

    private SubOutputInterface makeInterface(final String interfaceId) {
        final SubOutputInterface iface = new SubOutputInterface();
        iface.setInterfaceId(interfaceId);
        iface.setInterfaceUrl("http://bank.example.com/callback");
        iface.setAuthType(InterfaceAuthType.NONE);
        iface.setTimeoutSeconds(5);
        iface.setCallCount(0L);
        return iface;
    }

    @Test
    void poll_pendingEntity_httpSuccess_shouldMarkSendingThenDoneAndIncrementCallCount() {
        final CallbackQueueEntity entity = pendingEntity("q001", "if-001");
        final SubOutputInterface iface = makeInterface("if-001");

        when(callbackQueueRepository.claimBatch(anyInt())).thenReturn(List.of(entity.getQueueId()));
        when(callbackQueueRepository.findById(entity.getQueueId())).thenReturn(Optional.of(entity));
        when(subOutputInterfaceRepository.findById("if-001")).thenReturn(Optional.of(iface));
        when(httpClient.post(any(), any())).thenReturn(new CallbackResult(true, 200, null));

        runner.poll();

        // markSending save must happen BEFORE httpClient.post (InOrder)
        // save(SENDING) → post → save(DONE): verify first save is before post
        final InOrder order = inOrder(callbackQueueRepository, httpClient);
        order.verify(callbackQueueRepository).save(any(CallbackQueueEntity.class)); // markSending save
        order.verify(httpClient).post(any(), any());

        // Verify entity saved with DONE status (second save, after post)
        final ArgumentCaptor<CallbackQueueEntity> entityCaptor =
                ArgumentCaptor.forClass(CallbackQueueEntity.class);
        verify(callbackQueueRepository, atLeastOnce()).save(entityCaptor.capture());
        assertThat(entityCaptor.getAllValues())
                .anyMatch(e -> CallbackQueueStatus.DONE.equals(e.getStatus()));

        // Verify interface callCount incremented and saved
        final ArgumentCaptor<SubOutputInterface> ifaceCaptor =
                ArgumentCaptor.forClass(SubOutputInterface.class);
        verify(subOutputInterfaceRepository).save(ifaceCaptor.capture());
        assertThat(ifaceCaptor.getValue().getCallCount()).isEqualTo(1L);
        assertThat(ifaceCaptor.getValue().getLastCallTime()).isNotNull();

        // Verify metrics: recordSent called with a non-negative nanos value
        verify(metrics).recordSent(anyLong());
        verify(metrics, never()).recordRetry();
        verify(metrics, never()).recordDeadLetter();
    }

    @Test
    void poll_pendingEntity_httpNon2xx_shouldDelegateToRetryHandler() {
        final CallbackQueueEntity entity = pendingEntity("q002", "if-002");
        final SubOutputInterface iface = makeInterface("if-002");
        final CallbackResult result = new CallbackResult(false, 503, "http 503");

        when(callbackQueueRepository.claimBatch(anyInt())).thenReturn(List.of(entity.getQueueId()));
        when(callbackQueueRepository.findById(entity.getQueueId())).thenReturn(Optional.of(entity));
        when(subOutputInterfaceRepository.findById("if-002")).thenReturn(Optional.of(iface));
        when(httpClient.post(any(), any())).thenReturn(result);
        when(retryHandler.handleDeliveryFailure(any(), anyInt(), any()))
                .thenReturn(CallbackFailureOutcome.RETRY);

        runner.poll();

        // Delivery failure must be delegated to retryHandler (not directly markFailed)
        verify(retryHandler).handleDeliveryFailure(eq(entity), eq(iface.getRetryCount()), eq(result));
        // subOutputInterfaceRepository.save should NOT be called (no callCount writeback on failure)
        verify(subOutputInterfaceRepository, never()).save(any(SubOutputInterface.class));
        // Metrics: RETRY outcome → recordRetry, no recordSent/recordDeadLetter
        verify(metrics).recordRetry();
        verify(metrics, never()).recordSent(anyLong());
        verify(metrics, never()).recordDeadLetter();
    }

    @Test
    void poll_pendingEntity_httpFailure_deadLetterOutcome_shouldRecordDeadLetter() {
        final CallbackQueueEntity entity = pendingEntity("q004", "if-004");
        final SubOutputInterface iface = makeInterface("if-004");
        final CallbackResult result = new CallbackResult(false, 400, "bad request");

        when(callbackQueueRepository.claimBatch(anyInt())).thenReturn(List.of(entity.getQueueId()));
        when(callbackQueueRepository.findById(entity.getQueueId())).thenReturn(Optional.of(entity));
        when(subOutputInterfaceRepository.findById("if-004")).thenReturn(Optional.of(iface));
        when(httpClient.post(any(), any())).thenReturn(result);
        when(retryHandler.handleDeliveryFailure(any(), anyInt(), any()))
                .thenReturn(CallbackFailureOutcome.DEAD_LETTER);

        runner.poll();

        verify(metrics).recordDeadLetter();
        verify(metrics, never()).recordSent(anyLong());
        verify(metrics, never()).recordRetry();
    }

    @Test
    void poll_interfaceNotFound_shouldMarkFailedWithNotFoundMessage_andNotThrow() {
        final CallbackQueueEntity entity = pendingEntity("q003", "if-missing");

        when(callbackQueueRepository.claimBatch(anyInt())).thenReturn(List.of(entity.getQueueId()));
        when(callbackQueueRepository.findById(entity.getQueueId())).thenReturn(Optional.of(entity));
        when(subOutputInterfaceRepository.findById("if-missing")).thenReturn(Optional.empty());

        // Must NOT throw
        assertThatCode(() -> runner.poll()).doesNotThrowAnyException();

        final ArgumentCaptor<CallbackQueueEntity> entityCaptor =
                ArgumentCaptor.forClass(CallbackQueueEntity.class);
        verify(callbackQueueRepository, atLeastOnce()).save(entityCaptor.capture());
        // Last save must be FAILED (after markFailed)
        final CallbackQueueEntity last = entityCaptor.getAllValues()
                .get(entityCaptor.getAllValues().size() - 1);
        assertThat(last.getStatus()).isEqualTo(CallbackQueueStatus.FAILED);
        assertThat(last.getLastError()).containsIgnoringCase("interface not found");
        // httpClient must NOT be called
        verify(httpClient, never()).post(any(), any());
        // Config error: no delivery telemetry recorded
        verify(metrics, never()).recordSent(anyLong());
        verify(metrics, never()).recordRetry();
        verify(metrics, never()).recordDeadLetter();
    }

    @Test
    void poll_firstRowThrows_secondRowStillProcessed() {
        final CallbackQueueEntity bad = pendingEntity("q-bad", "if-bad");
        final CallbackQueueEntity good = pendingEntity("q-good", "if-good");
        final SubOutputInterface goodIface = makeInterface("if-good");

        when(callbackQueueRepository.claimBatch(anyInt()))
                .thenReturn(List.of(bad.getQueueId(), good.getQueueId()));
        // first row: findById returns entity but interface lookup blows up
        when(callbackQueueRepository.findById(bad.getQueueId())).thenReturn(Optional.of(bad));
        when(callbackQueueRepository.findById(good.getQueueId())).thenReturn(Optional.of(good));
        when(subOutputInterfaceRepository.findById("if-bad"))
                .thenThrow(new RuntimeException("boom"));
        // second row: healthy path
        when(subOutputInterfaceRepository.findById("if-good")).thenReturn(Optional.of(goodIface));
        when(httpClient.post(any(), any())).thenReturn(new CallbackResult(true, 200, null));

        // poll must not propagate the first row's RuntimeException
        assertThatCode(() -> runner.poll()).doesNotThrowAnyException();

        // second row was still processed despite first row failure (per-row isolation)
        verify(httpClient).post(eq(goodIface), any());
        final ArgumentCaptor<SubOutputInterface> ifaceCaptor =
                ArgumentCaptor.forClass(SubOutputInterface.class);
        verify(subOutputInterfaceRepository).save(ifaceCaptor.capture());
        assertThat(ifaceCaptor.getValue().getCallCount()).isEqualTo(1L);
    }

    @Test
    void poll_callsClaimBatchWithConfiguredBatchSize() {
        when(callbackQueueRepository.claimBatch(anyInt())).thenReturn(List.of());

        runner.poll();

        // claimBatch must be called with the configured batchSize
        verify(callbackQueueRepository).claimBatch(props.batchSize());
    }

    @Test
    void poll_entityNotFoundById_shouldSkipSilently() {
        // Simulate race: another instance already processed this id
        when(callbackQueueRepository.claimBatch(anyInt())).thenReturn(List.of("ghost-id"));
        when(callbackQueueRepository.findById("ghost-id")).thenReturn(Optional.empty());

        assertThatCode(() -> runner.poll()).doesNotThrowAnyException();

        // No http call, no interface lookup, no save (nothing to process)
        verify(httpClient, never()).post(any(), any());
        verify(subOutputInterfaceRepository, never()).findById(any());
        verify(callbackQueueRepository, never()).save(any());
    }
}
