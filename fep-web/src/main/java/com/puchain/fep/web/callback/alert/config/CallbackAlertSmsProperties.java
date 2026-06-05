package com.puchain.fep.web.callback.alert.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 回调告警 SMS 渠道配置（真实网关 URL / accessKey；secret 走 Nacos/环境变量，禁硬编码）。
 * 本 Plan log 桩不消费这些字段，预留供真实网关 Plan 使用。
 * 参见 PRD v1.3 §5.5.3（FR-INFRA-CALLBACK-ALERT）。
 *
 * @param gatewayUrl 网关地址
 * @param accessKey  访问 key
 * @author FEP Team
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "fep.callback.alert.sms")
public record CallbackAlertSmsProperties(String gatewayUrl, String accessKey) {
}
