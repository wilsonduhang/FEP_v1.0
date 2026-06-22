package com.puchain.fep.web.tlq.node.alert;

import com.puchain.fep.web.callback.alert.CallbackAlertMessage;
import com.puchain.fep.web.callback.alert.channel.CallbackAlertChannel;
import com.puchain.fep.web.sysmgmt.config.alert.domain.AlertFrequency;
import com.puchain.fep.web.sysmgmt.config.alert.domain.NotifyMethod;
import com.puchain.fep.web.sysmgmt.config.alert.domain.SysAlertRule;
import com.puchain.fep.web.sysmgmt.config.alert.repository.SysAlertRuleRepository;
import com.puchain.fep.web.tlq.node.event.TlqNodeOfflineEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Comparator;
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
 * {@link TlqNodeOfflineAlertEvaluator} 单元测试（镜像 {@code TlqOutboundAlertEvaluatorTest}）。
 *
 * <p>直调 listener 方法（不依赖 AFTER_COMMIT 事务基础设施），覆盖：分发到启用渠道且
 * category=TLQ_NODE_OFFLINE / 忽略 threshold / 无规则跳过 / 未启用跳过 / 渠道异常隔离。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class TlqNodeOfflineAlertEvaluatorTest {

    @Mock SysAlertRuleRepository ruleRepo;
    @Mock CallbackAlertChannel inApp;
    @Mock CallbackAlertChannel email;

    private TlqNodeOfflineEvent ev() {
        return new TlqNodeOfflineEvent("n1", "节点A", null, LocalDateTime.now());
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
        Set<NotifyMethod> s = new TreeSet<>(Comparator.comparing(Enum::name));
        s.addAll(List.of(ms));
        return s;
    }

    private TlqNodeOfflineAlertEvaluator evaluator() {
        lenient().when(inApp.supports(NotifyMethod.IN_APP)).thenReturn(true);
        lenient().when(inApp.supports(NotifyMethod.EMAIL)).thenReturn(false);
        lenient().when(email.supports(NotifyMethod.EMAIL)).thenReturn(true);
        lenient().when(email.supports(NotifyMethod.IN_APP)).thenReturn(false);
        return new TlqNodeOfflineAlertEvaluator(ruleRepo, List.of(inApp, email));
    }

    @Test
    void shouldDispatchWithNodeOfflineCategory() {
        when(ruleRepo.findAll()).thenReturn(List.of(
                rule(true, 0, methods(NotifyMethod.IN_APP, NotifyMethod.EMAIL))));
        evaluator().onTlqNodeOffline(ev());

        ArgumentCaptor<CallbackAlertMessage> cap = ArgumentCaptor.forClass(CallbackAlertMessage.class);
        verify(inApp).send(cap.capture());
        verify(email).send(any());
        assertThat(cap.getValue().category()).isEqualTo("TLQ_NODE_OFFLINE");
        assertThat(cap.getValue().refId()).isEqualTo("n1");
    }

    @Test
    void shouldAlertEvenWhenThresholdHigh() {
        // 节点离线忽略 threshold（决策 D2）：高 threshold 仍告警
        when(ruleRepo.findAll()).thenReturn(List.of(
                rule(true, 999, methods(NotifyMethod.IN_APP))));
        evaluator().onTlqNodeOffline(ev());
        verify(inApp).send(any());
    }

    @Test
    void shouldSkipWhenNoRuleConfigured() {
        when(ruleRepo.findAll()).thenReturn(List.of());
        TlqNodeOfflineAlertEvaluator e = evaluator();
        assertThatCode(() -> e.onTlqNodeOffline(ev())).doesNotThrowAnyException();
        verify(inApp, never()).send(any());
    }

    @Test
    void shouldSkipWhenDisabled() {
        when(ruleRepo.findAll()).thenReturn(List.of(
                rule(false, 0, methods(NotifyMethod.IN_APP))));
        evaluator().onTlqNodeOffline(ev());
        verify(inApp, never()).send(any());
    }

    @Test
    void shouldIsolateChannelException() {
        when(ruleRepo.findAll()).thenReturn(List.of(
                rule(true, 0, methods(NotifyMethod.IN_APP, NotifyMethod.EMAIL))));
        doThrow(new RuntimeException("boom")).when(inApp).send(any());
        TlqNodeOfflineAlertEvaluator e = evaluator();
        assertThatCode(() -> e.onTlqNodeOffline(ev())).doesNotThrowAnyException();
        verify(email).send(any());
    }
}
