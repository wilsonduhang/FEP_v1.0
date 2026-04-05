package com.puchain.fep.web.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * JPA 配置。
 *
 * <p>启用 JPA 审计（@CreatedDate / @LastModifiedDate 自动填充）
 * 和 Repository 扫描。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.puchain.fep.web")
@EnableJpaAuditing
public class JpaConfig {
}
