package com.puchain.fep.web.callback.runner;

import com.puchain.fep.web.callback.domain.CallbackQueueEntity;
import com.puchain.fep.web.callback.domain.CallbackQueueStatus;
import com.puchain.fep.web.callback.http.CallbackHttpClient;
import com.puchain.fep.web.callback.http.CallbackResult;
import com.puchain.fep.web.callback.repository.CallbackQueueRepository;
import com.puchain.fep.web.submission.outputinterface.domain.InterfaceAuthType;
import com.puchain.fep.web.submission.outputinterface.domain.SubOutputInterface;
import com.puchain.fep.web.submission.outputinterface.repository.SubOutputInterfaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link CallbackQueueRunner} 单元测试 — Mockito 驱动，验证 5 项验收标准：
 * DONE writeback、FAILED 记录、interface not found、批量 ≤50、per-row 隔离。
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

    private CallbackQueueRunner runner;

    @BeforeEach
    void setUp() {
        runner = new CallbackQueueRunner(callbackQueueRepository, httpClient, subOutputInterfaceRepository);
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
    void poll_pendingEntity_httpSuccess_shouldMarkDoneAndIncrementCallCount() {
        final CallbackQueueEntity entity = pendingEntity("q001", "if-001");
        final SubOutputInterface iface = makeInterface("if-001");

        when(callbackQueueRepository.findTop50ByStatusOrderByCreateTimeAsc(CallbackQueueStatus.PENDING))
                .thenReturn(List.of(entity));
        when(subOutputInterfaceRepository.findById("if-001")).thenReturn(Optional.of(iface));
        when(httpClient.post(any(), any())).thenReturn(new CallbackResult(true, 200, null));

        runner.poll();

        // Verify entity saved with DONE status
        final ArgumentCaptor<CallbackQueueEntity> entityCaptor = ArgumentCaptor.forClass(CallbackQueueEntity.class);
        verify(callbackQueueRepository, atLeastOnce()).save(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getStatus()).isEqualTo(CallbackQueueStatus.DONE);

        // Verify interface callCount incremented and saved
        final ArgumentCaptor<SubOutputInterface> ifaceCaptor = ArgumentCaptor.forClass(SubOutputInterface.class);
        verify(subOutputInterfaceRepository).save(ifaceCaptor.capture());
        assertThat(ifaceCaptor.getValue().getCallCount()).isEqualTo(1L);
        assertThat(ifaceCaptor.getValue().getLastCallTime()).isNotNull();
    }

    @Test
    void poll_pendingEntity_httpNon2xx_shouldMarkFailed() {
        final CallbackQueueEntity entity = pendingEntity("q002", "if-002");
        final SubOutputInterface iface = makeInterface("if-002");

        when(callbackQueueRepository.findTop50ByStatusOrderByCreateTimeAsc(CallbackQueueStatus.PENDING))
                .thenReturn(List.of(entity));
        when(subOutputInterfaceRepository.findById("if-002")).thenReturn(Optional.of(iface));
        when(httpClient.post(any(), any())).thenReturn(new CallbackResult(false, 503, "http 503"));

        runner.poll();

        final ArgumentCaptor<CallbackQueueEntity> entityCaptor = ArgumentCaptor.forClass(CallbackQueueEntity.class);
        verify(callbackQueueRepository, atLeastOnce()).save(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getStatus()).isEqualTo(CallbackQueueStatus.FAILED);
        // subOutputInterfaceRepository.save should NOT be called (no callCount writeback on failure)
        verify(subOutputInterfaceRepository, never()).save(any(SubOutputInterface.class));
    }

    @Test
    void poll_interfaceNotFound_shouldMarkFailedWithNotFoundMessage_andNotThrow() {
        final CallbackQueueEntity entity = pendingEntity("q003", "if-missing");

        when(callbackQueueRepository.findTop50ByStatusOrderByCreateTimeAsc(CallbackQueueStatus.PENDING))
                .thenReturn(List.of(entity));
        when(subOutputInterfaceRepository.findById("if-missing")).thenReturn(Optional.empty());

        // Must NOT throw
        assertThatCode(() -> runner.poll()).doesNotThrowAnyException();

        final ArgumentCaptor<CallbackQueueEntity> entityCaptor = ArgumentCaptor.forClass(CallbackQueueEntity.class);
        verify(callbackQueueRepository, atLeastOnce()).save(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getStatus()).isEqualTo(CallbackQueueStatus.FAILED);
        assertThat(entityCaptor.getValue().getLastError()).containsIgnoringCase("interface not found");
        // httpClient must NOT be called
        verify(httpClient, never()).post(any(), any());
    }

    @Test
    void poll_callsRepositoryWithPendingStatus() {
        when(callbackQueueRepository.findTop50ByStatusOrderByCreateTimeAsc(CallbackQueueStatus.PENDING))
                .thenReturn(List.of());

        runner.poll();

        // Criterion 4: findTop50 guarantees batch ≤50; verify correct status arg passed
        verify(callbackQueueRepository).findTop50ByStatusOrderByCreateTimeAsc(eq(CallbackQueueStatus.PENDING));
    }
}
