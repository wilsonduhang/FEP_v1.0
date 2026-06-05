package com.puchain.fep.web.callback.alert.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 回调告警 Email 渠道配置（发件人 from 地址；SMTP host/port/username/password 走标准
 * {@code spring.mail.*} 由 {@code JavaMailSender} 自动装配，密钥经 Nacos/环境变量注入，禁硬编码）。
 *
 * <p>参见 PRD v1.3 §5.5.3 回调可靠性告警（FR-INFRA-CALLBACK-ALERT）。</p>
 *
 * @param from 发件人地址（如 fep-alert@example.com）
 * @author FEP Team
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "fep.callback.alert.email")
public record CallbackAlertEmailProperties(String from) {
}
