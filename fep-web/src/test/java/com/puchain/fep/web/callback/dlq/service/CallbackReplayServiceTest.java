package com.puchain.fep.web.callback.dlq.service;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.web.callback.dlq.dto.DlqEntryResponse;
import com.puchain.fep.web.callback.dlq.dto.DlqReplayResponse;
import com.puchain.fep.web.callback.domain.CallbackQueueEntity;
import com.puchain.fep.web.callback.domain.CallbackQueueStatus;
import com.puchain.fep.web.callback.repository.CallbackQueueRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 单元测试 for {@link CallbackReplayService}。
 *
 * <p>mock {@link CallbackQueueRepository} 隔离持久层，验证复制重放语义（新 PENDING 行 +
 * 原死信行不变 + 审计回溯链字段）、非法状态/不存在抛 {@link FepBusinessException}、列表/链查询投影。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class CallbackReplayServiceTest {

    @Mock
    private CallbackQueueRepository repo;

    @InjectMocks
    private CallbackReplayService svc;

    private static CallbackQueueEntity deadLetterRow(final String queueId, final String interfaceId) {
        final CallbackQueueEntity e =
                CallbackQueueEntity.pending("idem-" + queueId, interfaceId, "2101", "{\"p\":1}");
        e.markDeadLetter(5, "fatal");
        ReflectionTestUtils.setField(e, "queueId", queueId);
        return e;
    }

    private static CallbackQueueEntity pendingRow(final String queueId, final String interfaceId) {
        final CallbackQueueEntity e =
                CallbackQueueEntity.pending("idem-" + queueId, interfaceId, "2101", "{\"p\":1}");
        ReflectionTestUtils.setField(e, "queueId", queueId);
        return e;
    }

    @Test
    void replayCreatesCopyAndLeavesOriginalUnchanged() {
        final CallbackQueueEntity dead = deadLetterRow("D1", "IF-001");
        when(repo.findById("D1")).thenReturn(Optional.of(dead));

        final DlqReplayResponse resp = svc.replay("D1", "admin-user-x");

        final ArgumentCaptor<CallbackQueueEntity> cap = ArgumentCaptor.forClass(CallbackQueueEntity.class);
        verify(repo).save(cap.capture());
        final CallbackQueueEntity copy = cap.getValue();
        assertThat(copy.getOriginalDlqId()).isEqualTo("D1");
        assertThat(copy.getStatus()).isEqualTo(CallbackQueueStatus.PENDING);
        assertThat(copy.getRetryCount()).isZero();
        assertThat(copy.getReplayedBy()).isEqualTo("admin-user-x");
        assertThat(copy.getReplayedAt()).isNotNull();
        // original 保留作审计证据，不变更
        assertThat(dead.getStatus()).isEqualTo(CallbackQueueStatus.DEAD_LETTER);
        assertThat(resp.newQueueId()).isEqualTo(copy.getQueueId());
        assertThat(resp.originalDlqId()).isEqualTo("D1");
        assertThat(resp.replayedAt()).isEqualTo(copy.getReplayedAt());
    }

    @Test
    void replayNonExistentThrowsBiz() {
        when(repo.findById("D-NOPE")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> svc.replay("D-NOPE", "u"))
                .isInstanceOf(FepBusinessException.class)
                .extracting("errorCode").isEqualTo(FepErrorCode.BIZ_5001);
        verify(repo, never()).save(any());
    }

    @Test
    void replayNonDeadLetterThrowsBiz() {
        final CallbackQueueEntity pending = pendingRow("P1", "IF-001");
        when(repo.findById("P1")).thenReturn(Optional.of(pending));
        assertThatThrownBy(() -> svc.replay("P1", "u"))
                .isInstanceOf(FepBusinessException.class)
                .extracting("errorCode").isEqualTo(FepErrorCode.BIZ_5001);
        verify(repo, never()).save(any());
    }

    @Test
    void listDeadLetterReturnsResponses() {
        final CallbackQueueEntity d1 = deadLetterRow("D1", "IF-001");
        final CallbackQueueEntity d2 = deadLetterRow("D2", "IF-002");
        when(repo.findDeadLetter(any())).thenReturn(List.of(d1, d2));

        final List<DlqEntryResponse> result = svc.list(PageRequest.of(0, 20));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).queueId()).isEqualTo("D1");
        assertThat(result.get(0).status()).isEqualTo(CallbackQueueStatus.DEAD_LETTER);
        assertThat(result.get(1).queueId()).isEqualTo("D2");
    }

    @Test
    void findReplayChainReturnsLinkedEntries() {
        final CallbackQueueEntity d2 = deadLetterRow("D2", "IF-001");
        ReflectionTestUtils.setField(d2, "originalDlqId", "D1");
        when(repo.findByOriginalDlqId("D1")).thenReturn(List.of(d2));

        final List<DlqEntryResponse> chain = svc.findReplayChain("D1");

        assertThat(chain).hasSize(1);
        assertThat(chain.get(0).queueId()).isEqualTo("D2");
        assertThat(chain.get(0).originalDlqId()).isEqualTo("D1");
    }
}
