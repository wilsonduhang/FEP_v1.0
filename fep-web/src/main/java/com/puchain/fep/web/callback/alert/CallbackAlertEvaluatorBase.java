package com.puchain.fep.web.callback.alert;

import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.web.callback.alert.channel.CallbackAlertChannel;
import com.puchain.fep.web.sysmgmt.config.alert.domain.AlertFrequency;
import com.puchain.fep.web.sysmgmt.config.alert.domain.NotifyMethod;
import com.puchain.fep.web.sysmgmt.config.alert.domain.SysAlertRule;
import com.puchain.fep.web.sysmgmt.config.alert.repository.SysAlertRuleRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

/**
 * 告警 evaluator 模板基类：单源化「规则门控 + 渠道隔离分发」不变量，供 callback DLQ /
 * TLQ 出站 DLQ / TLQ 节点离线三个告警 evaluator 复用（Rule-of-Three）。
 *
 * <p>放置于 {@code callback.alert} 包以满足 ArchUnit R4（callback.. 生产类须 {@code Callback}
 * 前缀）并复用同包共享词汇 {@link CallbackAlertMessage} / {@link CallbackAlertChannel}，避免包循环。
 * 基类泛型于事件类型 {@code T}，不依赖 {@code outbound} / {@code tlq.node} 包（R1/R3 安全）。</p>
 *
 * <p>子类保留各自的 {@code @EventListener} 或 {@link TransactionalEventListener}(AFTER_COMMIT)
 * 监听方法（注解类型不同无法上提，故监听方法留在子类，方法体仅一行 {@link #evaluate(Object)}）；
 * 并实现钩子 {@link #logger()} / {@link #refId(Object)} /
 * {@link #toAlertMessage(Object, String, String)} / {@link #alertContext()}，按需覆盖
 * {@link #passesThreshold(Object, SysAlertRule)}（默认恒 true，节点离线无计数语义）。
 * 频率 HOURLY/DAILY 汇总 deferred（当前按 REALTIME 立即分发）。</p>
 *
 * @param <T> 触发告警的事件类型
 * @author FEP Team
 * @since 1.0.0
 */
public abstract class CallbackAlertEvaluatorBase<T> {

    private final SysAlertRuleRepository ruleRepo;
    private final List<CallbackAlertChannel> channels;

    /**
     * @param ruleRepo 预警规则仓储（单条全局配置，三告警共用）
     * @param channels 全部告警渠道 bean（Spring 注入，三告警共用）
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Spring-managed singletons stored by reference per container contract")
    protected CallbackAlertEvaluatorBase(final SysAlertRuleRepository ruleRepo,
            final List<CallbackAlertChannel> channels) {
        this.ruleRepo = ruleRepo;
        this.channels = channels;
    }

    /**
     * 模板方法：按 {@code t_sys_alert_rule} 配置分发告警。无配置 / 未启用 / 未过阈值 → 安全返回。
     *
     * @param event 触发事件
     */
    @SuppressFBWarnings(value = "CRLF_INJECTION_LOGS",
            justification = "refId passed through LogSanitizer.sanitize() prior to LOG")
    protected final void evaluate(final T event) {
        final String refId = refId(event);
        final List<SysAlertRule> rules = ruleRepo.findAll();
        if (rules.isEmpty()) {
            logger().warn("{} but no alert rule configured, refId={}", alertContext(),
                    LogSanitizer.sanitize(refId));
            return;
        }
        final SysAlertRule rule = rules.get(0);
        if (!Boolean.TRUE.equals(rule.getAlertEnabled())) {
            return;
        }
        if (!passesThreshold(event, rule)) {
            return;
        }
        if (rule.getAlertFrequency() != AlertFrequency.REALTIME) {
            logger().debug("alert frequency {} aggregation deferred; dispatching as REALTIME, refId={}",
                    rule.getAlertFrequency(), LogSanitizer.sanitize(refId));
        }
        final CallbackAlertMessage msg = toAlertMessage(event, rule.getAlertEmail(),
                rule.getAlertPhone());
        dispatchAll(rule, msg, refId);
    }

    private void dispatchAll(final SysAlertRule rule, final CallbackAlertMessage msg,
            final String refId) {
        for (final NotifyMethod method : rule.getNotifyMethods()) {
            for (final CallbackAlertChannel ch : channels) {
                if (ch.supports(method)) {
                    dispatchIsolated(ch, msg, method, refId);
                }
            }
        }
    }

    @SuppressFBWarnings(value = "CRLF_INJECTION_LOGS",
            justification = "refId passed through LogSanitizer.sanitize() prior to LOG")
    private void dispatchIsolated(final CallbackAlertChannel ch, final CallbackAlertMessage msg,
            final NotifyMethod method, final String refId) {
        try {
            ch.send(msg);
        } catch (final RuntimeException ex) {
            logger().error("{} alert channel {} failed, refId={}", alertContext(), method,
                    LogSanitizer.sanitize(refId), ex);
        }
    }

    /**
     * @return 子类 logger（保留各自日志 category）
     */
    protected abstract Logger logger();

    /**
     * @param event 触发事件
     * @return 事件的引用 id（如 queueId / nodeId），用于日志与告警消息 refId
     */
    protected abstract String refId(T event);

    /**
     * @return 日志上下文名词（如 "callback DLQ" / "TLQ node offline" / "TLQ outbound DLQ"）
     */
    protected abstract String alertContext();

    /**
     * 组装通用告警消息（category 由子类决定）。
     *
     * @param event      触发事件
     * @param alertEmail EMAIL 收件邮箱（可 null）
     * @param alertPhone SMS 收件手机号（可 null）
     * @return 通用告警消息
     */
    protected abstract CallbackAlertMessage toAlertMessage(T event, String alertEmail,
            String alertPhone);

    /**
     * 阈值钩子：默认恒 {@code true}（离散事件无计数语义）。计数类告警（DLQ retryCount）覆盖。
     *
     * @param event 触发事件
     * @param rule  生效规则
     * @return true 则继续分发
     */
    protected boolean passesThreshold(final T event, final SysAlertRule rule) {
        return true;
    }
}
