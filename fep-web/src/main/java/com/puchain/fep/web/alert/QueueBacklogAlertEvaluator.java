package com.puchain.fep.web.alert;

import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.web.callback.alert.CallbackAlertEvaluatorBase;
import com.puchain.fep.web.callback.alert.CallbackAlertMessage;
import com.puchain.fep.web.callback.alert.channel.CallbackAlertChannel;
import com.puchain.fep.web.sysmgmt.config.alert.repository.SysAlertRuleRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 队列积压告警引擎：订阅 {@link QueueBacklogEvent}，经 {@link CallbackAlertEvaluatorBase} 模板按
 * {@code t_sys_alert_rule} 配置分发到各 {@link CallbackAlertChannel}（category=QUEUE_BACKLOG）。
 *
 * <p>放置于中立 {@code web.alert} 包：其监控器需同时读 callback + outbound 两队列，不能落在
 * {@code callback.alert}（违 ArchUnit R1「callback 不依赖 outbound」）。复用 callback.alert 的基类 +
 * 通用消息记录（Web 层内依赖合法）。积压是运维 {@code WARN} 级信号（区别于 DLQ/节点离线的
 * {@code ERROR}）。阈值门控已由监控器边沿触发完成，故不覆盖 {@code passesThreshold}（基类默认恒
 * true）。参见 PRD v1.3 §5.9.1 告警管理（FR-WEB-TLQ-FAULT）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
@SuppressFBWarnings(value = "CRLF_INJECTION_LOGS",
        justification = "queue name passed through LogSanitizer.sanitize() prior to LOG in toAlertMessage")
public class QueueBacklogAlertEvaluator extends CallbackAlertEvaluatorBase<QueueBacklogEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(QueueBacklogAlertEvaluator.class);
    private static final String LEVEL_WARN = "WARN";
    private static final String CATEGORY_QUEUE_BACKLOG = "QUEUE_BACKLOG";
    private static final String REF_TYPE_QUEUE_BACKLOG = "QUEUE_BACKLOG";

    /**
     * @param ruleRepo 预警规则仓储（单条全局配置，与其他告警共用）
     * @param channels 全部告警渠道 bean（Spring 注入，与其他告警共用）
     */
    public QueueBacklogAlertEvaluator(final SysAlertRuleRepository ruleRepo,
            final List<CallbackAlertChannel> channels) {
        super(ruleRepo, channels);
    }

    /**
     * 处理队列积压事件：委托模板。无配置 / 未启用 → 安全返回不告警。
     *
     * @param ev 队列积压事件
     */
    @EventListener
    public void onQueueBacklog(final QueueBacklogEvent ev) {
        evaluate(ev);
    }

    @Override
    protected Logger logger() {
        return LOG;
    }

    @Override
    protected String refId(final QueueBacklogEvent ev) {
        return ev.queue().name();
    }

    @Override
    protected String alertContext() {
        return "queue backlog";
    }

    /**
     * 组装积压告警消息（category=QUEUE_BACKLOG，level=WARN）。
     *
     * @param ev         队列积压事件
     * @param alertEmail EMAIL 收件邮箱（可 null）
     * @param alertPhone SMS 收件手机号（可 null）
     * @return 通用告警消息
     */
    @Override
    protected CallbackAlertMessage toAlertMessage(final QueueBacklogEvent ev,
            final String alertEmail, final String alertPhone) {
        final String queue = ev.queue().name();
        final String title = "队列积压告警 - " + queue;
        final String body = LogSanitizer.sanitize(String.format(
                "queue=%s backlog=%d threshold=%d", queue, ev.backlogDepth(), ev.threshold()));
        return new CallbackAlertMessage(CATEGORY_QUEUE_BACKLOG, LEVEL_WARN, title, body,
                queue, REF_TYPE_QUEUE_BACKLOG, alertEmail, alertPhone);
    }
}
