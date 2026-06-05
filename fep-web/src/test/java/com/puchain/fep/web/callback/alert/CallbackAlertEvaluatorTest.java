package com.puchain.fep.web.callback.alert;

import com.puchain.fep.web.callback.alert.channel.CallbackAlertChannel;
import com.puchain.fep.web.callback.dlq.event.CallbackDeadLetterEvent;
import com.puchain.fep.web.sysmgmt.config.alert.domain.AlertFrequency;
import com.puchain.fep.web.sysmgmt.config.alert.domain.NotifyMethod;
import com.puchain.fep.web.sysmgmt.config.alert.domain.SysAlertRule;
import com.puchain.fep.web.sysmgmt.config.alert.repository.SysAlertRuleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link CallbackAlertEvaluator} 单元测试。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class CallbackAlertEvaluatorTest {

    @Mock SysAlertRuleRepository ruleRepo;
    @Mock CallbackAlertChannel inApp;
    @Mock CallbackAlertChannel email;

    private CallbackDeadLetterEvent ev(final int retryCount) {
        return new CallbackDeadLetterEvent("q1", "IF-1", "9120", retryCount, "HTTP 500",
                LocalDateTime.now());
    }

    private SysAlertRule rule(final boolean enabled, final int threshold,
            final Set<NotifyMethod> methods) {
        SysAlertRule r = new SysAlertRule();
        r.setAlertEnabled(enabled);
        r.setThreshold(threshold);
        r.setNotifyMethods(methods);
        r.setAlertEmail("ops@bank.com");
        r.setAlertFrequency(AlertFrequency.REALTIME);
        return r;
    }

    private Set<NotifyMethod> methods(final NotifyMethod... ms) {
        Set<NotifyMethod> s = new TreeSet<>(java.util.Comparator.comparing(Enum::name));
        s.addAll(List.of(ms));
        return s;
    }

    private CallbackAlertEvaluator evaluator() {
        // 全部 supports 桩用 lenient：skip 分支（disabled/below-threshold）不触发 channel，
        // strict stubbing 会因未用桩抛 UnnecessaryStubbingException。
        lenient().when(inApp.supports(NotifyMethod.IN_APP)).thenReturn(true);
        lenient().when(inApp.supports(NotifyMethod.EMAIL)).thenReturn(false);
        lenient().when(email.supports(NotifyMethod.EMAIL)).thenReturn(true);
        lenient().when(email.supports(NotifyMethod.IN_APP)).thenReturn(false);
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
