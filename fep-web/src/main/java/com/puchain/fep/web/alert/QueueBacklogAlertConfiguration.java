package com.puchain.fep.web.alert;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 启用 {@link QueueBacklogAlertProperties} 绑定（DEF-B9-3）。
 *
 * <p>命名以 {@code Configuration} 结尾满足 {@code NamingConventionTest}（@Configuration 类名约束）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Configuration
@EnableConfigurationProperties(QueueBacklogAlertProperties.class)
public class QueueBacklogAlertConfiguration {
}
