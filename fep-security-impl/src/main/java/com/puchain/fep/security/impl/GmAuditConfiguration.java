package com.puchain.fep.security.impl;

import com.puchain.fep.security.api.AuditIntegrityService;
import com.puchain.fep.security.api.HashService;
import com.puchain.fep.security.api.KeyService;
import com.puchain.fep.security.api.SignService;
import com.puchain.fep.security.impl.audit.AuditIntegrityServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 审计完整性装配 — always-on（hash 链恒真 SM3；签名实现随容器内 SignService bean
 * 自适应 mock/impl，无 @ConditionalOnProperty——镜像 GmHashConfiguration）。
 *
 * <p>实现类 {@link AuditIntegrityServiceImpl} 无 Spring stereotype，经本类 {@code @Bean}
 * 注册（红线 feedback_provider_switch_impl_no_stereotype_bean_registration）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Configuration
public class GmAuditConfiguration {

    /**
     * 审计完整性原语（always-on 单例）。
     *
     * @param hashService SM3
     * @param signService SM2（mock/impl 随 provider）
     * @param keyService  审计密钥
     * @return AuditIntegrityService 实现
     */
    @Bean
    public AuditIntegrityService auditIntegrityService(final HashService hashService,
            final SignService signService, final KeyService keyService) {
        return new AuditIntegrityServiceImpl(hashService, signService, keyService);
    }
}
