package com.puchain.fep.web.callback.alert.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 回调告警渠道配置属性注册（对齐 {@code CallbackConsumerConfiguration} 的
 * {@code @EnableConfigurationProperties} 范式）。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Configuration
@EnableConfigurationProperties({CallbackAlertEmailProperties.class, CallbackAlertSmsProperties.class})
public class CallbackAlertConfiguration {
}
