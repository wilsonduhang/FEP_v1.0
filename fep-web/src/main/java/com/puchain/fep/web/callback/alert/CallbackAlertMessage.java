package com.puchain.fep.web.callback.alert;

import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.web.callback.dlq.event.CallbackDeadLetterEvent;
import com.puchain.fep.web.outbound.event.TlqOutboundDeadLetterEvent;

/**
 * 统一告警消息：由各 evaluator（{@code CallbackAlertEvaluator} / {@code TlqOutboundAlertEvaluator}）
 * 从死信事件与 {@code SysAlertRule} 收件人配置组装，分发给各 {@code CallbackAlertChannel}。
 *
 * <p>{@code category} 决定站内通知落库的来源分类（IN_APP 渠道按此区分 callback / TLQ 等告警源，
 * 供告警历史查询）。{@code body} 在工厂中经 {@link LogSanitizer#sanitize(String)} 处理，去除 CRLF
 * 注入风险（质量门禁 #4）。参见 PRD v1.3 §5.5.3 回调可靠性告警（FR-INFRA-CALLBACK-ALERT）
 * 与 §5.7/§5.9.1 TLQ 故障告警（FR-WEB-TLQ-FAULT）。</p>
 *
 * @param category   告警来源分类（站内通知 category，如 CALLBACK_DLQ / TLQ_OUTBOUND_DLQ）
 * @param level      级别（ERROR/WARN/INFO）
 * @param title      标题
 * @param body       正文（已 sanitize）
 * @param refId      关联业务对象 id（死信 queueId）
 * @param refType    关联业务对象类型
 * @param alertEmail EMAIL 渠道收件邮箱（可 null）
 * @param alertPhone SMS 渠道收件手机号（可 null）
 * @author FEP Team
 * @since 1.0.0
 */
public record CallbackAlertMessage(
        String category, String level, String title, String body,
        String refId, String refType, String alertEmail, String alertPhone) {

    private static final String LEVEL_ERROR = "ERROR";
    private static final String CATEGORY_CALLBACK_DLQ = "CALLBACK_DLQ";
    private static final String REF_TYPE_DLQ = "CALLBACK_DLQ_ENTRY";
    private static final String CATEGORY_TLQ_OUTBOUND = "TLQ_OUTBOUND_DLQ";
    private static final String REF_TYPE_TLQ_OUTBOUND = "TLQ_OUTBOUND_DLQ_ENTRY";

    /**
     * 从回调死信事件组装告警消息（category=CALLBACK_DLQ）。
     *
     * @param ev         回调死信事件
     * @param alertEmail EMAIL 收件邮箱（可 null）
     * @param alertPhone SMS 收件手机号（可 null）
     * @return 告警消息
     */
    public static CallbackAlertMessage ofDeadLetter(final CallbackDeadLetterEvent ev,
            final String alertEmail, final String alertPhone) {
        final String title = "回调死信 - " + ev.targetInterfaceId();
        final String body = LogSanitizer.sanitize(String.format(
                "queueId=%s msgNo=%s retryCount=%d error=%s",
                ev.queueId(), ev.msgNo(), ev.retryCount(), ev.lastError()));
        return new CallbackAlertMessage(CATEGORY_CALLBACK_DLQ, LEVEL_ERROR, title, body,
                ev.queueId(), REF_TYPE_DLQ, alertEmail, alertPhone);
    }

    /**
     * 从 TLQ 出站死信事件组装告警消息（category=TLQ_OUTBOUND_DLQ）。
     *
     * @param ev         TLQ 出站死信事件
     * @param alertEmail EMAIL 收件邮箱（可 null）
     * @param alertPhone SMS 收件手机号（可 null）
     * @return 告警消息
     */
    public static CallbackAlertMessage ofTlqOutboundDeadLetter(final TlqOutboundDeadLetterEvent ev,
            final String alertEmail, final String alertPhone) {
        final String title = "TLQ 出站死信 - " + ev.queueId();
        final String body = LogSanitizer.sanitize(String.format(
                "queueId=%s msgNo=%s retryCount=%d error=%s",
                ev.queueId(), ev.msgNo(), ev.retryCount(), ev.lastError()));
        return new CallbackAlertMessage(CATEGORY_TLQ_OUTBOUND, LEVEL_ERROR, title, body,
                ev.queueId(), REF_TYPE_TLQ_OUTBOUND, alertEmail, alertPhone);
    }
}
