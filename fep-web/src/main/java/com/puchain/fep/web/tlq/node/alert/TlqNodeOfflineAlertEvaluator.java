package com.puchain.fep.web.tlq.node.alert;

import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.web.callback.alert.CallbackAlertEvaluatorBase;
import com.puchain.fep.web.callback.alert.CallbackAlertMessage;
import com.puchain.fep.web.callback.alert.channel.CallbackAlertChannel;
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
 * TLQ 节点离线告警引擎：订阅 {@link TlqNodeOfflineEvent}，经 {@link CallbackAlertEvaluatorBase}
 * 模板按 {@code t_sys_alert_rule} 配置（启用/频率/渠道集合/收件人）分发到各 {@link CallbackAlertChannel}
 * （category=TLQ_NODE_OFFLINE → IN_APP 经 {@code CallbackInAppAlertChannel} 落
 * {@code in_app_notification} → 复用 B-8 实时推送链）。
 *
 * <p>两点对计数类告警的合理差异：① 用 {@link TransactionalEventListener}(AFTER_COMMIT) 而非
 * {@code @EventListener}——发布点 {@code TlqNodeService.changeStatus} 是 {@code @Transactional}，
 * 须待提交后告警以免节点状态回滚致假告警（与 B-8 {@code DashboardNotificationPushListener} 一致）；
 * ② 不覆盖 {@code passesThreshold}（用基类默认恒 true）——节点离线是离散单次事件无计数语义
 * （threshold 仅对 DLQ retryCount 有意义）。频率 HOURLY/DAILY 汇总 deferred（当前按 REALTIME
 * 立即分发）。参见 PRD v1.3 §5.7 TLQ 节点管理 › 故障处理（FR-WEB-TLQ-FAULT）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
@SuppressFBWarnings(value = "CRLF_INJECTION_LOGS",
        justification = "nodeId/nodeName passed through LogSanitizer.sanitize() prior to LOG in toAlertMessage")
public class TlqNodeOfflineAlertEvaluator extends CallbackAlertEvaluatorBase<TlqNodeOfflineEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(TlqNodeOfflineAlertEvaluator.class);
    private static final String LEVEL_ERROR = "ERROR";
    private static final String CATEGORY_NODE_OFFLINE = "TLQ_NODE_OFFLINE";
    private static final String REF_TYPE_NODE = "TLQ_NODE";

    /**
     * @param ruleRepo 预警规则仓储（单条全局配置，与 callback / TLQ 出站告警共用）
     * @param channels 全部告警渠道 bean（Spring 注入，与 callback / TLQ 出站告警共用）
     */
    public TlqNodeOfflineAlertEvaluator(final SysAlertRuleRepository ruleRepo,
            final List<CallbackAlertChannel> channels) {
        super(ruleRepo, channels);
    }

    /**
     * 处理 TLQ 节点离线事件：委托模板。无配置 / 未启用 → 安全返回不告警。
     *
     * <p>{@code AFTER_COMMIT}：仅当 {@code changeStatus} 事务成功提交后触发，避免假告警。</p>
     *
     * @param ev 节点离线事件
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTlqNodeOffline(final TlqNodeOfflineEvent ev) {
        evaluate(ev);
    }

    @Override
    protected Logger logger() {
        return LOG;
    }

    @Override
    protected String refId(final TlqNodeOfflineEvent ev) {
        return ev.nodeId();
    }

    @Override
    protected String alertContext() {
        return "TLQ node offline";
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
    @Override
    protected CallbackAlertMessage toAlertMessage(final TlqNodeOfflineEvent ev,
            final String alertEmail, final String alertPhone) {
        final String title = "TLQ 节点离线 - " + ev.nodeName();
        final String body = LogSanitizer.sanitize(String.format(
                "nodeId=%s nodeName=%s lastHeartbeat=%s occurredAt=%s",
                ev.nodeId(), ev.nodeName(), ev.lastHeartbeat(), ev.occurredAt()));
        return new CallbackAlertMessage(CATEGORY_NODE_OFFLINE, LEVEL_ERROR, title, body,
                ev.nodeId(), REF_TYPE_NODE, alertEmail, alertPhone);
    }
}
