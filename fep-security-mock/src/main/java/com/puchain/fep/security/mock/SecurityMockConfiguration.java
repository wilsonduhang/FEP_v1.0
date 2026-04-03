package com.puchain.fep.security.mock;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * FEP Security Mock 模块配置。
 *
 * <p>仅在 {@code dev} profile 下激活，注册 Mock 安全服务实现。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Configuration
@Profile("dev")
public class SecurityMockConfiguration {
}
