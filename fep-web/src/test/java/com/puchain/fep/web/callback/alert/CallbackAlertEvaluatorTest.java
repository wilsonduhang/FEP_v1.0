package com.puchain.fep.web.callback.alert;

import com.puchain.fep.web.alert.AbstractAlertEvaluatorTest;
import com.puchain.fep.web.callback.dlq.event.CallbackDeadLetterEvent;
import com.puchain.fep.web.sysmgmt.config.alert.domain.NotifyMethod;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link CallbackAlertEvaluator} 单元测试。共享 mock 字段与 fixture helper 见
 * {@link AbstractAlertEvaluatorTest}。
 *
 * @author FEP Team
 * @since 1.0.0
 */
class CallbackAlertEvaluatorTest extends AbstractAlertEvaluatorTest {

    private CallbackDeadLetterEvent ev(final int retryCount) {
        return new CallbackDeadLetterEvent("q1", "IF-1", "9120", retryCount, "HTTP 500",
                LocalDateTime.now());
    }

    private CallbackAlertEvaluator evaluator() {
        stubChannelSupports();
        return new CallbackAlertEvaluator(ruleRepo, List.of(inApp, email));
    }

    @Test
    void shouldDispatchToEnabledChannels() {
        when(ruleRepo.findAll()).thenReturn(List.of(
                rule(true, 0, methods(NotifyMethod.IN_APP, NotifyMethod.EMAIL))));
        evaluator().onDeadLetter(ev(3));
        verify(inApp).send(any());
        verify(email).send(any());
    }

    @Test
    void shouldSkipWhenNoRuleConfigured() {
        when(ruleRepo.findAll()).thenReturn(List.of());
        CallbackAlertEvaluator e = evaluator();
        assertThatCode(() -> e.onDeadLetter(ev(3))).doesNotThrowAnyException();
        verify(inApp, never()).send(any());
    }

    @Test
    void shouldSkipWhenDisabled() {
        when(ruleRepo.findAll()).thenReturn(List.of(
                rule(false, 0, methods(NotifyMethod.IN_APP))));
        evaluator().onDeadLetter(ev(3));
        verify(inApp, never()).send(any());
    }

    @Test
    void shouldSkipBelowThreshold() {
        when(ruleRepo.findAll()).thenReturn(List.of(
                rule(true, 5, methods(NotifyMethod.IN_APP))));
        evaluator().onDeadLetter(ev(3));
        verify(inApp, never()).send(any());
    }

    @Test
    void shouldIsolateChannelException() {
        when(ruleRepo.findAll()).thenReturn(List.of(
                rule(true, 0, methods(NotifyMethod.IN_APP, NotifyMethod.EMAIL))));
        doThrow(new RuntimeException("boom")).when(inApp).send(any());
        CallbackAlertEvaluator e = evaluator();
        assertThatCode(() -> e.onDeadLetter(ev(3))).doesNotThrowAnyException();
        verify(email).send(any());
    }
}
