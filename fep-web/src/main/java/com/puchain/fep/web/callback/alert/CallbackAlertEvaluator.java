package com.puchain.fep.web.callback.alert;

import com.puchain.fep.web.callback.alert.channel.CallbackAlertChannel;
import com.puchain.fep.web.callback.dlq.event.CallbackDeadLetterEvent;
import com.puchain.fep.web.sysmgmt.config.alert.domain.SysAlertRule;
import com.puchain.fep.web.sysmgmt.config.alert.repository.SysAlertRuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Callback 死信告警引擎：订阅 {@link CallbackDeadLetterEvent}，经 {@link CallbackAlertEvaluatorBase}
 * 模板按 {@code t_sys_alert_rule} 配置（启用/阈值/频率/渠道集合/收件人）分发到各
 * {@link CallbackAlertChannel}。
 *
 * <p>替代 Phase 2b {@code CallbackNotificationListener}（IN_APP 硬编码），将 IN_APP 纳入配置门控。
 * 单渠道异常隔离由基类保证；频率 HOURLY/DAILY 汇总窗口 deferred（当前按 REALTIME 立即分发）。
 * 参见 PRD v1.3 §5.5.3 回调可靠性告警（FR-INFRA-CALLBACK-ALERT）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class CallbackAlertEvaluator extends CallbackAlertEvaluatorBase<CallbackDeadLetterEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(CallbackAlertEvaluator.class);

    /**
     * @param ruleRepo 预警规则仓储（单条全局配置）
     * @param channels 全部告警渠道 bean（Spring 注入）
     */
    public CallbackAlertEvaluator(final SysAlertRuleRepository ruleRepo,
            final List<CallbackAlertChannel> channels) {
        super(ruleRepo, channels);
    }

    /**
     * 处理死信事件：委托模板。无配置 / 未启用 / 未达阈值 → 安全返回不告警。
     *
     * @param ev 死信事件
     */
    @EventListener
    public void onDeadLetter(final CallbackDeadLetterEvent ev) {
        evaluate(ev);
    }

    @Override
    protected Logger logger() {
        return LOG;
    }

    @Override
    protected String refId(final CallbackDeadLetterEvent ev) {
        return ev.queueId();
    }

    @Override
    protected String alertContext() {
        return "callback DLQ";
    }

    @Override
    protected boolean passesThreshold(final CallbackDeadLetterEvent ev, final SysAlertRule rule) {
        final int threshold = rule.getThreshold() == null ? 0 : rule.getThreshold();
        return ev.retryCount() >= threshold;
    }

    @Override
    protected CallbackAlertMessage toAlertMessage(final CallbackDeadLetterEvent ev,
            final String alertEmail, final String alertPhone) {
        return CallbackAlertMessage.ofDeadLetter(ev, alertEmail, alertPhone);
    }
}
