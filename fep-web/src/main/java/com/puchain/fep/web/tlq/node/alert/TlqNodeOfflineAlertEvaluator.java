package com.puchain.fep.web.tlq.node.alert;

import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.web.callback.alert.CallbackAlertMessage;
import com.puchain.fep.web.callback.alert.channel.CallbackAlertChannel;
import com.puchain.fep.web.sysmgmt.config.alert.domain.AlertFrequency;
import com.puchain.fep.web.sysmgmt.config.alert.domain.NotifyMethod;
import com.puchain.fep.web.sysmgmt.config.alert.domain.SysAlertRule;
import com.puchain.fep.web.sysmgmt.config.alert.repository.SysAlertRuleRepository;
import com.puchain.fep.web.tlq.node.event.TlqNodeOfflineEvent;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

/**
 * TLQ 节点离线告警引擎：单一监听器订阅 {@link TlqNodeOfflineEvent}，按 {@code t_sys_alert_rule}
 * 配置（启用/频率/渠道集合/收件人）分发到各 {@link CallbackAlertChannel}。
 *
 * <p>镜像 {@code TlqOutboundAlertEvaluator}，复用同一全局告警规则与渠道集合（IN_APP 经
 * {@code CallbackInAppAlertChannel} 落 {@code in_app_notification} category=TLQ_NODE_OFFLINE →
 * 自动复用 B-8 实时推送链）。两点对 Phase-1 的合理偏离：① 用
 * {@link TransactionalEventListener}(AFTER_COMMIT) 而非 {@code @EventListener}——发布点
 * {@code TlqNodeService.changeStatus} 是 {@code @Transactional}，须待提交后告警以免节点状态回滚
 * 致假告警（与 B-8 {@code DashboardNotificationPushListener} 一致）；② 忽略 threshold——节点
 * 离线是离散单次事件无计数语义（threshold 仅对 DLQ retryCount 有意义）。频率 HOURLY/DAILY 汇总
 * deferred（当前按 REALTIME 立即分发）。参见 PRD v1.3 §5.7 TLQ 节点管理 › 故障处理
 * （FR-WEB-TLQ-FAULT）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
@SuppressFBWarnings(value = "CRLF_INJECTION_LOGS",
        justification = "nodeId/nodeName passed through LogSanitizer.sanitize() prior to LOG")
public class TlqNodeOfflineAlertEvaluator {

    private static final Logger LOG = LoggerFactory.getLogger(TlqNodeOfflineAlertEvaluator.class);
    private static final String LEVEL_ERROR = "ERROR";
    private static final String CATEGORY_NODE_OFFLINE = "TLQ_NODE_OFFLINE";
    private static final String REF_TYPE_NODE = "TLQ_NODE";

    private final SysAlertRuleRepository ruleRepo;
    private final List<CallbackAlertChannel> channels;

    /**
     * @param ruleRepo 预警规则仓储（单条全局配置，与 callback / TLQ 出站告警共用）
     * @param channels 全部告警渠道 bean（Spring 注入，与 callback / TLQ 出站告警共用）
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Spring-managed singletons stored by reference per container contract")
    public TlqNodeOfflineAlertEvaluator(final SysAlertRuleRepository ruleRepo,
            final List<CallbackAlertChannel> channels) {
        this.ruleRepo = ruleRepo;
        this.channels = channels;
    }

    /**
     * 处理 TLQ 节点离线事件：按配置分发告警。无配置 / 未启用 → 安全返回不告警。
     *
     * <p>{@code AFTER_COMMIT}：仅当 {@code changeStatus} 事务成功提交后触发，避免假告警。</p>
     *
     * @param ev 节点离线事件
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTlqNodeOffline(final TlqNodeOfflineEvent ev) {
        final List<SysAlertRule> rules = ruleRepo.findAll();
        if (rules.isEmpty()) {
            LOG.warn("TLQ node offline event but no alert rule configured, nodeId={}",
                    LogSanitizer.sanitize(ev.nodeId()));
            return;
        }
        final SysAlertRule rule = rules.get(0);
        if (!Boolean.TRUE.equals(rule.getAlertEnabled())) {
            return;
        }
        if (rule.getAlertFrequency() != AlertFrequency.REALTIME) {
            LOG.debug("alert frequency {} aggregation deferred; dispatching as REALTIME, nodeId={}",
                    rule.getAlertFrequency(), LogSanitizer.sanitize(ev.nodeId()));
        }
        final CallbackAlertMessage msg = toAlertMessage(ev, rule.getAlertEmail(), rule.getAlertPhone());
        for (final NotifyMethod method : rule.getNotifyMethods()) {
            for (final CallbackAlertChannel ch : channels) {
                if (ch.supports(method)) {
                    dispatchIsolated(ch, msg, method, ev.nodeId());
                }
            }
        }
    }

    /**
     * 从 TLQ 节点离线事件组装通用告警消息（category=TLQ_NODE_OFFLINE）。
     *
     * <p>构造置于本 evaluator（{@code tlq.node.alert} 包）而非 {@code CallbackAlertMessage}
     * 工厂，以遵守 ArchUnit R1（callback 包不得依赖业务模块）；{@code CallbackAlertMessage}
     * 作为通用记录被复用。{@code body} 经 {@link LogSanitizer#sanitize(String)} 去 CRLF 注入风险。</p>
     *
     * @param ev         TLQ 节点离线事件
     * @param alertEmail EMAIL 收件邮箱（可 null）
     * @param alertPhone SMS 收件手机号（可 null）
     * @return 通用告警消息
     */
    private CallbackAlertMessage toAlertMessage(final TlqNodeOfflineEvent ev,
            final String alertEmail, final String alertPhone) {
        final String title = "TLQ 节点离线 - " + ev.nodeName();
        final String body = LogSanitizer.sanitize(String.format(
                "nodeId=%s nodeName=%s lastHeartbeat=%s occurredAt=%s",
                ev.nodeId(), ev.nodeName(), ev.lastHeartbeat(), ev.occurredAt()));
        return new CallbackAlertMessage(CATEGORY_NODE_OFFLINE, LEVEL_ERROR, title, body,
                ev.nodeId(), REF_TYPE_NODE, alertEmail, alertPhone);
    }

    private void dispatchIsolated(final CallbackAlertChannel ch, final CallbackAlertMessage msg,
            final NotifyMethod method, final String nodeId) {
        try {
            ch.send(msg);
        } catch (final RuntimeException ex) {
            LOG.error("TLQ node offline alert channel {} failed, nodeId={}", method,
                    LogSanitizer.sanitize(nodeId), ex);
        }
    }
}
