package com.puchain.fep.web.outbound.alert;

import com.puchain.fep.web.callback.alert.CallbackAlertMessage;
import com.puchain.fep.web.callback.alert.channel.CallbackAlertChannel;
import com.puchain.fep.web.outbound.event.TlqOutboundDeadLetterEvent;
import com.puchain.fep.web.sysmgmt.config.alert.domain.AlertFrequency;
import com.puchain.fep.web.sysmgmt.config.alert.domain.NotifyMethod;
import com.puchain.fep.web.sysmgmt.config.alert.domain.SysAlertRule;
import com.puchain.fep.web.sysmgmt.config.alert.repository.SysAlertRuleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link TlqOutboundAlertEvaluator} 单元测试（镜像 {@code CallbackAlertEvaluatorTest}）。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class TlqOutboundAlertEvaluatorTest {

    @Mock SysAlertRuleRepository ruleRepo;
    @Mock CallbackAlertChannel inApp;
    @Mock CallbackAlertChannel email;

    private TlqOutboundDeadLetterEvent ev(final int retryCount) {
        return new TlqOutboundDeadLetterEvent("q1", null, retryCount, "send fail",
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

    private TlqOutboundAlertEvaluator evaluator() {
        lenient().when(inApp.supports(NotifyMethod.IN_APP)).thenReturn(true);
        lenient().when(inApp.supports(NotifyMethod.EMAIL)).thenReturn(false);
        lenient().when(email.supports(NotifyMethod.EMAIL)).thenReturn(true);
        lenient().when(email.supports(NotifyMethod.IN_APP)).thenReturn(false);
        return new TlqOutboundAlertEvaluator(ruleRepo, List.of(inApp, email));
    }

    @Test
    void shouldDispatchToEnabledChannelsWithTlqCategory() {
        when(ruleRepo.findAll()).thenReturn(List.of(
                rule(true, 0, methods(NotifyMethod.IN_APP, NotifyMethod.EMAIL))));
        evaluator().onTlqOutboundDeadLetter(ev(5));

        ArgumentCaptor<CallbackAlertMessage> cap = ArgumentCaptor.forClass(CallbackAlertMessage.class);
        verify(inApp).send(cap.capture());
        verify(email).send(any());
        assertThat(cap.getValue().category()).isEqualTo("TLQ_OUTBOUND_DLQ");
        assertThat(cap.getValue().refId()).isEqualTo("q1");
    }

    @Test
    void shouldSkipWhenNoRuleConfigured() {
        when(ruleRepo.findAll()).thenReturn(List.of());
        TlqOutboundAlertEvaluator e = evaluator();
        assertThatCode(() -> e.onTlqOutboundDeadLetter(ev(5))).doesNotThrowAnyException();
        verify(inApp, never()).send(any());
    }

    @Test
    void shouldSkipWhenDisabled() {
        when(ruleRepo.findAll()).thenReturn(List.of(
                rule(false, 0, methods(NotifyMethod.IN_APP))));
        evaluator().onTlqOutboundDeadLetter(ev(5));
        verify(inApp, never()).send(any());
    }

    @Test
    void shouldSkipBelowThreshold() {
        when(ruleRepo.findAll()).thenReturn(List.of(
                rule(true, 10, methods(NotifyMethod.IN_APP))));
        evaluator().onTlqOutboundDeadLetter(ev(5));
        verify(inApp, never()).send(any());
    }

    @Test
    void shouldIsolateChannelException() {
        when(ruleRepo.findAll()).thenReturn(List.of(
                rule(true, 0, methods(NotifyMethod.IN_APP, NotifyMethod.EMAIL))));
        doThrow(new RuntimeException("boom")).when(inApp).send(any());
        TlqOutboundAlertEvaluator e = evaluator();
        assertThatCode(() -> e.onTlqOutboundDeadLetter(ev(5))).doesNotThrowAnyException();
        verify(email).send(any());
    }
}
