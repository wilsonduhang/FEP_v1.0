package com.puchain.fep.web.callback.alert.channel;

import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.web.callback.alert.CallbackAlertMessage;
import com.puchain.fep.web.callback.alert.sms.CallbackSmsGateway;
import com.puchain.fep.web.sysmgmt.config.alert.domain.NotifyMethod;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * SMS 告警渠道：委派 {@link CallbackSmsGateway} 向 {@code SysAlertRule.alertPhone} 发短信。
 * 网关异常由网关实现自隔离；本渠道仅做收件人空值守卫。参见 PRD v1.3 §5.5.3
 * （FR-INFRA-CALLBACK-ALERT）。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
@SuppressFBWarnings(value = "CRLF_INJECTION_LOGS",
        justification = "phone passed through LogSanitizer.sanitize() prior to LOG")
public class CallbackSmsAlertChannel implements CallbackAlertChannel {

    private static final Logger LOG = LoggerFactory.getLogger(CallbackSmsAlertChannel.class);

    private final CallbackSmsGateway gateway;

    /**
     * @param gateway SMS 网关（dev/CI 为 log 桩）
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Spring-managed singleton stored by reference per container contract")
    public CallbackSmsAlertChannel(final CallbackSmsGateway gateway) {
        this.gateway = gateway;
    }

    @Override
    public boolean supports(final NotifyMethod method) {
        return method == NotifyMethod.SMS;
    }

    @Override
    public void send(final CallbackAlertMessage message) {
        final String phone = message.alertPhone();
        if (phone == null || phone.isBlank()) {
            LOG.warn("SMS alert configured but alertPhone is blank, refId={}",
                    LogSanitizer.sanitize(message.refId()));
            return;
        }
        gateway.send(phone, message.body());
    }
}
