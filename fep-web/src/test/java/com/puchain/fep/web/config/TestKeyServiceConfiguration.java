package com.puchain.fep.web.config;

import com.puchain.fep.security.api.KeyService;
import com.puchain.fep.security.mock.MockKeyService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Test configuration providing a {@link KeyService} bean for integration tests.
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Configuration
@ConditionalOnProperty(prefix = "fep.security", name = "provider", havingValue = "mock",
        matchIfMissing = true)
public class TestKeyServiceConfiguration {

    /**
     * Returns the shared dev {@link MockKeyService}（fep-security-mock，compile-scope 依赖）。
     *
     * <p>直接 {@code new} 实例化绕过其 {@code @Profile("dev")} bean 装配门（注解对直接
     * 实例化惰性），消除此前 ~50 行逐方法镜像匿名类（Simplify R4）。仅在 context 无其他
     * {@link KeyService} bean 时注册；dev profile 下 fep-security-mock 的 bean 优先，
     * 本 bean 让位使 {@code @MockBean KeyService} 注入无歧义。</p>
     *
     * @return test KeyService implementation
     */
    @Bean
    @ConditionalOnMissingBean(KeyService.class)
    public KeyService keyService() {
        return new MockKeyService();
    }
}
