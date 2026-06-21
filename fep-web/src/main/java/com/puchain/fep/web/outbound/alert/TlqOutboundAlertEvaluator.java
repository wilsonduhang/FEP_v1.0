package com.puchain.fep.web.outbound.alert;

import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.web.callback.alert.CallbackAlertMessage;
import com.puchain.fep.web.callback.alert.channel.CallbackAlertChannel;
import com.puchain.fep.web.outbound.event.TlqOutboundDeadLetterEvent;
import com.puchain.fep.web.sysmgmt.config.alert.domain.AlertFrequency;
import com.puchain.fep.web.sysmgmt.config.alert.domain.NotifyMethod;
import com.puchain.fep.web.sysmgmt.config.alert.domain.SysAlertRule;
import com.puchain.fep.web.sysmgmt.config.alert.repository.SysAlertRuleRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * TLQ 出站告警引擎：单一 {@link EventListener} 订阅 {@link TlqOutboundDeadLetterEvent}，按
 * {@code t_sys_alert_rule} 配置（启用/阈值/频率/渠道集合/收件人）分发到各 {@link CallbackAlertChannel}。
 *
 * <p>镜像 callback 侧 {@code CallbackAlertEvaluator}，复用同一全局告警规则与渠道集合（IN_APP 经
 * {@code CallbackInAppAlertChannel} 落 {@code in_app_notification} category=TLQ_OUTBOUND_DLQ →
 * 自动复用 B-8 实时推送链）。单渠道异常隔离；频率 HOURLY/DAILY 汇总窗口 deferred（当前按 REALTIME
 * 立即分发，与 callback 侧一致）。参见 PRD v1.3 §5.7 TLQ 节点管理 › 故障处理 / §5.9.1 告警管理
 * （FR-WEB-TLQ-FAULT）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
@SuppressFBWarnings(value = "CRLF_INJECTION_LOGS",
        justification = "queueId passed through LogSanitizer.sanitize() prior to LOG")
public class TlqOutboundAlertEvaluator {

    private static final Logger LOG = LoggerFactory.getLogger(TlqOutboundAlertEvaluator.class);

    private final SysAlertRuleRepository ruleRepo;
    private final List<CallbackAlertChannel> channels;

    /**
     * @param ruleRepo 预警规则仓储（单条全局配置，与 callback 告警共用）
     * @param channels 全部告警渠道 bean（Spring 注入，与 callback 告警共用）
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Spring-managed singletons stored by reference per container contract")
    public TlqOutboundAlertEvaluator(final SysAlertRuleRepository ruleRepo,
            final List<CallbackAlertChannel> channels) {
        this.ruleRepo = ruleRepo;
        this.channels = channels;
    }

    /**
     * 处理 TLQ 出站死信事件：按配置分发告警。无配置 / 未启用 / 未达阈值 → 安全返回不告警。
     *
     * @param ev TLQ 出站死信事件
     */
    @EventListener
    public void onTlqOutboundDeadLetter(final TlqOutboundDeadLetterEvent ev) {
        final List<SysAlertRule> rules = ruleRepo.findAll();
        if (rules.isEmpty()) {
            LOG.warn("TLQ outbound DLQ event but no alert rule configured, queueId={}",
                    LogSanitizer.sanitize(ev.queueId()));
            return;
        }
        final SysAlertRule rule = rules.get(0);
        if (!Boolean.TRUE.equals(rule.getAlertEnabled())) {
            return;
        }
        final int threshold = rule.getThreshold() == null ? 0 : rule.getThreshold();
        if (ev.retryCount() < threshold) {
            return;
        }
        if (rule.getAlertFrequency() != AlertFrequency.REALTIME) {
            LOG.debug("alert frequency {} aggregation deferred; dispatching as REALTIME, queueId={}",
                    rule.getAlertFrequency(), LogSanitizer.sanitize(ev.queueId()));
        }
        final CallbackAlertMessage msg = CallbackAlertMessage.ofTlqOutboundDeadLetter(
                ev, rule.getAlertEmail(), rule.getAlertPhone());
        for (final NotifyMethod method : rule.getNotifyMethods()) {
            for (final CallbackAlertChannel ch : channels) {
                if (ch.supports(method)) {
                    dispatchIsolated(ch, msg, method, ev.queueId());
                }
            }
        }
    }

    private void dispatchIsolated(final CallbackAlertChannel ch, final CallbackAlertMessage msg,
            final NotifyMethod method, final String queueId) {
        try {
            ch.send(msg);
        } catch (final RuntimeException ex) {
            LOG.error("TLQ alert channel {} failed, queueId={}", method,
                    LogSanitizer.sanitize(queueId), ex);
        }
    }
}
