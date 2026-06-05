package com.puchain.fep.web.callback.alert;

import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.web.callback.dlq.event.CallbackDeadLetterEvent;

/**
 * 统一告警消息：由 {@code CallbackAlertEvaluator} 从 {@link CallbackDeadLetterEvent} 与
 * {@code SysAlertRule} 收件人配置组装，分发给各 {@code CallbackAlertChannel}。
 *
 * <p>{@code body} 在工厂中经 {@link LogSanitizer#sanitize(String)} 处理，去除 CRLF 注入风险
 * （质量门禁 #4）。参见 PRD v1.3 §5.5.3 回调可靠性告警（FR-INFRA-CALLBACK-ALERT）。</p>
 *
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
        String level, String title, String body,
        String refId, String refType, String alertEmail, String alertPhone) {

    private static final String LEVEL_ERROR = "ERROR";
    private static final String REF_TYPE_DLQ = "CALLBACK_DLQ_ENTRY";

    /**
     * 从死信事件组装告警消息。
     *
     * @param ev         死信事件
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
        return new CallbackAlertMessage(LEVEL_ERROR, title, body,
                ev.queueId(), REF_TYPE_DLQ, alertEmail, alertPhone);
    }
}
