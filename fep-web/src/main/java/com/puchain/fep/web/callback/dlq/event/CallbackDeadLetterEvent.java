package com.puchain.fep.web.callback.dlq.event;

import java.time.LocalDateTime;

/**
 * 回调死信事件：当一条回调队列条目进入 {@code DEAD_LETTER} 终态时由
 * {@code CallbackRetryHandler} 发布（Spring {@code ApplicationEventPublisher}），
 * 由 {@code CallbackAlertEvaluator}（Phase 2c-A 统一告警引擎）订阅，按 t_sys_alert_rule 分发 IN_APP/EMAIL/SMS。
 *
 * <p>事件解耦设计（决策门 6）：RetryHandler 不直接依赖通知模块，仅 publishEvent；
 * 订阅方按需扩展（IN_APP / 未来 EMAIL / SMS）而不改 RetryHandler。</p>
 *
 * <p>参见 PRD v1.3 §5.10.7.2d 告警（FR-INFRA-CALLBACK-IN-APP-ALERT）。</p>
 *
 * @param queueId           死信行 queue_id（PK）
 * @param targetInterfaceId 目标输出接口 id
 * @param msgNo             报文号
 * @param retryCount        进入死信时的累计重试次数
 * @param lastError         最后错误摘要（已截断 ≤500）
 * @param occurredAt        死信发生时间
 * @author FEP Team
 * @since 1.0.0
 */
public record CallbackDeadLetterEvent(
        String queueId,
        String targetInterfaceId,
        String msgNo,
        int retryCount,
        String lastError,
        LocalDateTime occurredAt) {
}
