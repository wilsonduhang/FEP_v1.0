package com.puchain.fep.web.callback.alert.sms;

import com.puchain.fep.common.util.LogSanitizer;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * SMS 网关 log 桩（dev/CI 默认）：不发真实短信，仅记录脱敏后的发送意图。真实网关 bean 引入时
 * 经 {@link ConditionalOnMissingBean} 自动让位（约定 bean 名 {@code realSmsGateway}）。
 * 手机号经 {@link LogSanitizer} 脱敏（质量门禁 #4）。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
@ConditionalOnMissingBean(name = "realSmsGateway")
@SuppressFBWarnings(value = "CRLF_INJECTION_LOGS",
        justification = "phone passed through LogSanitizer.sanitize() prior to LOG")
public class CallbackLoggingSmsGateway implements CallbackSmsGateway {

    private static final Logger LOG = LoggerFactory.getLogger(CallbackLoggingSmsGateway.class);

    @Override
    public void send(final String phone, final String content) {
        LOG.info("[SMS-STUB] would send to phone={} contentLen={}",
                LogSanitizer.sanitize(phone), content == null ? 0 : content.length());
    }
}
