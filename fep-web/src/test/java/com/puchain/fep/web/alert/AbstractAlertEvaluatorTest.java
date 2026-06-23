package com.puchain.fep.web.alert;

import com.puchain.fep.web.callback.alert.channel.CallbackAlertChannel;
import com.puchain.fep.web.sysmgmt.config.alert.domain.AlertFrequency;
import com.puchain.fep.web.sysmgmt.config.alert.domain.NotifyMethod;
import com.puchain.fep.web.sysmgmt.config.alert.domain.SysAlertRule;
import com.puchain.fep.web.sysmgmt.config.alert.repository.SysAlertRuleRepository;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.mockito.Mockito.lenient;

/**
 * 告警 evaluator 单元测试基类：单源化三个告警 evaluator 测试（callback DLQ / TLQ 出站 DLQ /
 * TLQ 节点离线）共享的 mock 字段与 fixture helper（{@code rule()} / {@code methods()} /
 * {@code stubChannelSupports()}）。
 *
 * <p>放置于 {@code web.alert} 测试包（非 {@code callback..}）以避开 ArchUnit R4 命名约束
 * （{@code CallbackModuleArchTest} 扫测试类）。各子类提供各自的事件工厂 {@code ev()} 与
 * {@code evaluator()} 构造（具体 evaluator 类型不同，无法上提）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
public abstract class AbstractAlertEvaluatorTest {

    @Mock
    protected SysAlertRuleRepository ruleRepo;
    @Mock
    protected CallbackAlertChannel inApp;
    @Mock
    protected CallbackAlertChannel email;

    /**
     * 构造单条全局告警规则。
     *
     * @param enabled   是否启用
     * @param threshold 阈值（计数类用；离散事件忽略）
     * @param methods   渠道集合
     * @return 规则实体
     */
    protected SysAlertRule rule(final boolean enabled, final int threshold,
            final Set<NotifyMethod> methods) {
        final SysAlertRule r = new SysAlertRule();
        r.setAlertEnabled(enabled);
        r.setThreshold(threshold);
        r.setNotifyMethods(methods);
        r.setAlertEmail("ops@bank.com");
        r.setAlertFrequency(AlertFrequency.REALTIME);
        return r;
    }

    /**
     * 构造确定序的渠道方法集合。
     *
     * @param ms 渠道方法
     * @return 集合
     */
    protected Set<NotifyMethod> methods(final NotifyMethod... ms) {
        final Set<NotifyMethod> s = new TreeSet<>(Comparator.comparing(Enum::name));
        s.addAll(List.of(ms));
        return s;
    }

    /**
     * lenient 桩：inApp 支持 IN_APP、email 支持 EMAIL（skip 分支不触发渠道，避免
     * {@code UnnecessaryStubbingException}）。子类 {@code evaluator()} 构造前调用。
     */
    protected void stubChannelSupports() {
        lenient().when(inApp.supports(NotifyMethod.IN_APP)).thenReturn(true);
        lenient().when(inApp.supports(NotifyMethod.EMAIL)).thenReturn(false);
        lenient().when(email.supports(NotifyMethod.EMAIL)).thenReturn(true);
        lenient().when(email.supports(NotifyMethod.IN_APP)).thenReturn(false);
    }
}
