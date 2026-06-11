package com.puchain.fep.security.impl;

import com.puchain.fep.security.api.HashService;
import com.puchain.fep.security.impl.hash.HashServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SM3 摘要装配 — always-on（纯哈希无密钥/无 mock/无 provider 之分，镜像 DesensitizeConfiguration）。
 *
 * <p>实现类 {@link HashServiceImpl} 无 Spring stereotype，经本类 {@code @Bean} 注册。
 * 不带 @ConditionalOnProperty——SM3 摘要 dev/prod 同算法始终启用。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Configuration
public class GmHashConfiguration {

    /**
     * SM3 摘要服务（always-on 单例）。
     *
     * @return HashService 实现
     */
    @Bean
    public HashService hashService() {
        return new HashServiceImpl();
    }
}
