package com.puchain.fep.web.outbound.alert;

import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.web.callback.alert.CallbackAlertEvaluatorBase;
import com.puchain.fep.web.callback.alert.CallbackAlertMessage;
import com.puchain.fep.web.callback.alert.channel.CallbackAlertChannel;
import com.puchain.fep.web.outbound.event.TlqOutboundDeadLetterEvent;
import com.puchain.fep.web.sysmgmt.config.alert.domain.SysAlertRule;
import com.puchain.fep.web.sysmgmt.config.alert.repository.SysAlertRuleRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * TLQ 出站告警引擎：订阅 {@link TlqOutboundDeadLetterEvent}，经 {@link CallbackAlertEvaluatorBase}
 * 模板按 {@code t_sys_alert_rule} 配置分发到各 {@link CallbackAlertChannel}（category=TLQ_OUTBOUND_DLQ
 * → IN_APP 经 {@code CallbackInAppAlertChannel} 落 {@code in_app_notification} → 复用 B-8 实时推送链）。
 *
 * <p>单渠道异常隔离由基类保证；频率 HOURLY/DAILY 汇总窗口 deferred（当前按 REALTIME 立即分发）。
 * 参见 PRD v1.3 §5.7 TLQ 节点管理 › 故障处理 / §5.9.1 告警管理（FR-WEB-TLQ-FAULT）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
@SuppressFBWarnings(value = "CRLF_INJECTION_LOGS",
        justification = "queueId passed through LogSanitizer.sanitize() prior to LOG in toAlertMessage")
public class TlqOutboundAlertEvaluator extends CallbackAlertEvaluatorBase<TlqOutboundDeadLetterEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(TlqOutboundAlertEvaluator.class);
    private static final String LEVEL_ERROR = "ERROR";
    private static final String CATEGORY_TLQ_OUTBOUND = "TLQ_OUTBOUND_DLQ";
    private static final String REF_TYPE_TLQ_OUTBOUND = "TLQ_OUTBOUND_DLQ_ENTRY";

    /**
     * @param ruleRepo 预警规则仓储（单条全局配置，与 callback 告警共用）
     * @param channels 全部告警渠道 bean（Spring 注入，与 callback 告警共用）
     */
    public TlqOutboundAlertEvaluator(final SysAlertRuleRepository ruleRepo,
            final List<CallbackAlertChannel> channels) {
        super(ruleRepo, channels);
    }

    /**
     * 处理 TLQ 出站死信事件：委托模板。无配置 / 未启用 / 未达阈值 → 安全返回不告警。
     *
     * @param ev TLQ 出站死信事件
     */
    @EventListener
    public void onTlqOutboundDeadLetter(final TlqOutboundDeadLetterEvent ev) {
        evaluate(ev);
    }

    @Override
    protected Logger logger() {
        return LOG;
    }

    @Override
    protected String refId(final TlqOutboundDeadLetterEvent ev) {
        return ev.queueId();
    }

    @Override
    protected String alertContext() {
        return "TLQ outbound DLQ";
    }

    @Override
    protected boolean passesThreshold(final TlqOutboundDeadLetterEvent ev, final SysAlertRule rule) {
        final int threshold = rule.getThreshold() == null ? 0 : rule.getThreshold();
        return ev.retryCount() >= threshold;
    }

    /**
     * 从 TLQ 出站死信事件组装通用告警消息（category=TLQ_OUTBOUND_DLQ）。
     *
     * <p>构造置于本 evaluator（{@code outbound.alert} 包）而非 {@code CallbackAlertMessage}
     * 工厂，以遵守 ArchUnit R1（callback 包不得依赖 outbound 包）；{@code CallbackAlertMessage}
     * 作为通用记录被复用。{@code body} 经 {@link LogSanitizer#sanitize(String)} 去 CRLF 注入风险。</p>
     *
     * @param ev         TLQ 出站死信事件
     * @param alertEmail EMAIL 收件邮箱（可 null）
     * @param alertPhone SMS 收件手机号（可 null）
     * @return 通用告警消息
     */
    @Override
    protected CallbackAlertMessage toAlertMessage(final TlqOutboundDeadLetterEvent ev,
            final String alertEmail, final String alertPhone) {
        final String title = "TLQ 出站死信 - " + ev.queueId();
        final String body = LogSanitizer.sanitize(String.format(
                "queueId=%s msgNo=%s retryCount=%d error=%s",
                ev.queueId(), ev.msgNo(), ev.retryCount(), ev.lastError()));
        return new CallbackAlertMessage(CATEGORY_TLQ_OUTBOUND, LEVEL_ERROR, title, body,
                ev.queueId(), REF_TYPE_TLQ_OUTBOUND, alertEmail, alertPhone);
    }
}
