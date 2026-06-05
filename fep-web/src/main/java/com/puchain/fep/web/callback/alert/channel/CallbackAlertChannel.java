package com.puchain.fep.web.callback.alert.channel;

import com.puchain.fep.web.callback.alert.CallbackAlertMessage;
import com.puchain.fep.web.sysmgmt.config.alert.domain.NotifyMethod;

/**
 * 告警渠道策略接口：每个实现声明所支持的 {@link NotifyMethod}，由
 * {@code CallbackAlertEvaluator} 按配置启用的渠道集合分发。
 *
 * <p>实现：{@code CallbackInAppAlertChannel}（IN_APP）、{@code CallbackEmailAlertChannel}（EMAIL）、
 * {@code CallbackSmsAlertChannel}（SMS）。参见 PRD v1.3 §5.5.3（FR-INFRA-CALLBACK-ALERT）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public interface CallbackAlertChannel {

    /**
     * @param method 通知方式
     * @return 本渠道是否支持该通知方式
     */
    boolean supports(NotifyMethod method);

    /**
     * 发送告警（实现须自隔离异常，不向上抛以免影响其他渠道与死信主流程）。
     *
     * @param message 统一告警消息
     */
    void send(CallbackAlertMessage message);
}
