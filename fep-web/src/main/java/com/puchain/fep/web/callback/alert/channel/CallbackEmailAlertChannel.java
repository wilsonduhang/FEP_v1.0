package com.puchain.fep.web.callback.alert.channel;

import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.web.callback.alert.CallbackAlertMessage;
import com.puchain.fep.web.callback.alert.config.CallbackAlertEmailProperties;
import com.puchain.fep.web.sysmgmt.config.alert.domain.NotifyMethod;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * EMAIL 告警渠道：经 {@link JavaMailSender} 向 {@code SysAlertRule.alertEmail} 发送纯文本告警。
 *
 * <p>SMTP 连接参数走标准 {@code spring.mail.*}（密钥 Nacos/环境变量注入，禁硬编码）；发件人 from
 * 由 {@link CallbackAlertEmailProperties} 提供。{@code MailException} 自隔离不上抛（渠道异常不
 * 影响其他渠道与死信主流程）。参见 PRD v1.3 §5.5.3（FR-INFRA-CALLBACK-ALERT）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
@SuppressFBWarnings(value = "CRLF_INJECTION_LOGS",
        justification = "recipient passed through LogSanitizer.sanitize() prior to LOG")
public class CallbackEmailAlertChannel implements CallbackAlertChannel {

    private static final Logger LOG = LoggerFactory.getLogger(CallbackEmailAlertChannel.class);

    private final JavaMailSender mailSender;
    private final CallbackAlertEmailProperties props;

    /**
     * @param mailSender Spring 邮件发送器
     * @param props      Email 渠道配置（发件人 from）
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Spring-managed singletons stored by reference per container contract")
    public CallbackEmailAlertChannel(final JavaMailSender mailSender,
            final CallbackAlertEmailProperties props) {
        this.mailSender = mailSender;
        this.props = props;
    }

    @Override
    public boolean supports(final NotifyMethod method) {
        return method == NotifyMethod.EMAIL;
    }

    @Override
    public void send(final CallbackAlertMessage message) {
        final String to = message.alertEmail();
        if (to == null || to.isBlank()) {
            LOG.warn("EMAIL alert configured but alertEmail is blank, refId={}",
                    LogSanitizer.sanitize(message.refId()));
            return;
        }
        final SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(props.from());
        mail.setTo(to);
        mail.setSubject(message.title());
        mail.setText(message.body());
        try {
            mailSender.send(mail);
            LOG.info("DLQ alert email sent, refId={}", LogSanitizer.sanitize(message.refId()));
        } catch (final RuntimeException ex) {
            // 渠道自隔离：MailException 及 mailSender 实现层任何 RuntimeException 均不上抛，
            // 履行 CallbackAlertChannel 契约（不影响其他渠道与死信主流程）。
            LOG.error("DLQ alert email failed, refId={}", LogSanitizer.sanitize(message.refId()), ex);
        }
    }
}
