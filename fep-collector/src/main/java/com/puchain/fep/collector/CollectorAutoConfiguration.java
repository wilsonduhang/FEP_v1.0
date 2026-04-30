package com.puchain.fep.collector;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * FEP Collector 模块自动配置。
 *
 * <p>P4 T0：注册 {@link CollectorProperties}。后续 Task 在此追加适配器/调度/组装/Repository 等 Bean。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Configuration
@EnableConfigurationProperties(CollectorProperties.class)
public class CollectorAutoConfiguration {
}
