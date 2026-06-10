package com.puchain.fep.security.impl;

import com.puchain.fep.security.api.DesensitizeService;
import com.puchain.fep.security.impl.desensitize.DesensitizeServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 数据脱敏装配 — always-on（脱敏无密钥/无 mock/无 provider 之分，dev/prod 同规则）。
 *
 * <p>实现类 {@link DesensitizeServiceImpl} 无 Spring stereotype，经本类 {@code @Bean} 注册，
 * 避免被 fep-web 广扫 {@code @ComponentScan("com.puchain.fep")} 误识 + 规避命名 ArchUnit
 * （红线 feedback_provider_switch_impl_no_stereotype_bean_registration）。不带 @ConditionalOnProperty
 * （与 GmSecurityConfiguration 的 provider 门控不同——脱敏始终启用）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Configuration
public class DesensitizeConfiguration {

    /**
     * 数据脱敏服务（always-on 单例）。
     *
     * @return DesensitizeService 实现
     */
    @Bean
    public DesensitizeService desensitizeService() {
        return new DesensitizeServiceImpl();
    }
}
