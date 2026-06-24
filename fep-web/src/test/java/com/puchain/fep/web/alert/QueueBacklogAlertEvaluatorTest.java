package com.puchain.fep.web.alert;

import com.puchain.fep.web.callback.alert.CallbackAlertMessage;
import com.puchain.fep.web.sysmgmt.config.alert.domain.NotifyMethod;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link QueueBacklogAlertEvaluator} 单元测试（镜像 {@code TlqOutboundAlertEvaluatorTest}）。
 * 共享 mock 字段与 fixture helper 见 {@link AbstractAlertEvaluatorTest}。
 *
 * @author FEP Team
 * @since 1.0.0
 */
class QueueBacklogAlertEvaluatorTest extends AbstractAlertEvaluatorTest {

    private QueueBacklogEvent ev(final QueueBacklogQueue queue, final long depth) {
        return new QueueBacklogEvent(queue, depth, 1000L, LocalDateTime.now());
    }

    private QueueBacklogAlertEvaluator evaluator() {
        stubChannelSupports();
        return new QueueBacklogAlertEvaluator(ruleRepo, List.of(inApp, email));
    }

    @Test
    void shouldDispatchToEnabledChannelsWithBacklogCategory() {
        when(ruleRepo.findAll()).thenReturn(List.of(
                rule(true, 0, methods(NotifyMethod.IN_APP, NotifyMethod.EMAIL))));
        evaluator().onQueueBacklog(ev(QueueBacklogQueue.CALLBACK, 1500L));

        final ArgumentCaptor<CallbackAlertMessage> cap =
                ArgumentCaptor.forClass(CallbackAlertMessage.class);
        verify(inApp).send(cap.capture());
        verify(email).send(any());
        assertThat(cap.getValue().category()).isEqualTo("QUEUE_BACKLOG");
        assertThat(cap.getValue().level()).isEqualTo("WARN");
        assertThat(cap.getValue().refId()).isEqualTo("CALLBACK");
        assertThat(cap.getValue().body()).contains("backlog=1500", "threshold=1000");
    }

    @Test
    void shouldSkipWhenNoRuleConfigured() {
        when(ruleRepo.findAll()).thenReturn(List.of());
        final QueueBacklogAlertEvaluator e = evaluator();
        assertThatCode(() -> e.onQueueBacklog(ev(QueueBacklogQueue.OUTBOUND, 9999L)))
                .doesNotThrowAnyException();
        verify(inApp, never()).send(any());
    }

    @Test
    void shouldSkipWhenDisabled() {
        when(ruleRepo.findAll()).thenReturn(List.of(
                rule(false, 0, methods(NotifyMethod.IN_APP))));
        evaluator().onQueueBacklog(ev(QueueBacklogQueue.CALLBACK, 5000L));
        verify(inApp, never()).send(any());
    }

    @Test
    void shouldIsolateChannelException() {
        when(ruleRepo.findAll()).thenReturn(List.of(
                rule(true, 0, methods(NotifyMethod.IN_APP, NotifyMethod.EMAIL))));
        doThrow(new RuntimeException("boom")).when(inApp).send(any());
        final QueueBacklogAlertEvaluator e = evaluator();
        assertThatCode(() -> e.onQueueBacklog(ev(QueueBacklogQueue.OUTBOUND, 2000L)))
                .doesNotThrowAnyException();
        verify(email).send(any());
    }
}
