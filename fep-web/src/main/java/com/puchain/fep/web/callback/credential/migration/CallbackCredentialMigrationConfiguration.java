package com.puchain.fep.web.callback.credential.migration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 凭证惰性迁移配置 — 启用 {@link CallbackLegacyCredentialKeyIdProperties} 绑定
 * （前缀 {@code fep.callback.credential.migration}，与项目 {@code @EnableConfigurationProperties} 范式一致）。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Configuration
@EnableConfigurationProperties(CallbackLegacyCredentialKeyIdProperties.class)
public class CallbackCredentialMigrationConfiguration {
}
