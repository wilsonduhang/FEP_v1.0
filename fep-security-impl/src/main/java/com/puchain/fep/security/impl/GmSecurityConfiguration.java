package com.puchain.fep.security.impl;

import com.puchain.fep.security.api.CryptoService;
import com.puchain.fep.security.api.KeyService;
import com.puchain.fep.security.impl.crypto.BouncyCastleGmProviderConfig;
import com.puchain.fep.security.impl.crypto.CryptoServiceImpl;
import com.puchain.fep.security.impl.key.FepSecurityKeyProperties;
import com.puchain.fep.security.impl.key.FepSecuritySm2Properties;
import com.puchain.fep.security.impl.key.KeyServiceImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 国密真实实现装配 — 仅当 {@code fep.security.provider=impl} 时启用。
 *
 * <p>默认/dev 不激活（走 fep-security-mock 的 {@code @ConditionalOnProperty(provider=mock,
 * matchIfMissing=true)}），保证零回归 + 任一时刻单 {@link CryptoService}/{@link KeyService} bean。</p>
 *
 * <p>impl 实现类（{@link CryptoServiceImpl}/{@link KeyServiceImpl}/{@link BouncyCastleGmProviderConfig}）
 * 不带 Spring stereotype，统一经本类 {@code @Bean} 注册，避免被 fep-web 广扫
 * （{@code FepApplication @ComponentScan("com.puchain.fep")}）在 provider≠impl 时误装配，
 * 并规避命名约定 ArchUnit（{@code *Impl}/{@code *Config} 非 {@code *Service}/{@code *Configuration}）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Configuration
@ConditionalOnProperty(prefix = "fep.security", name = "provider", havingValue = "impl")
@EnableConfigurationProperties({FepSecurityKeyProperties.class, FepSecuritySm2Properties.class})
public class GmSecurityConfiguration {

    /**
     * 注册 BouncyCastle GM provider（构造即幂等注册 "BC"）。
     *
     * @return provider 注册器
     */
    @Bean
    public BouncyCastleGmProviderConfig bouncyCastleGmProviderConfig() {
        return new BouncyCastleGmProviderConfig();
    }

    /**
     * SM4/ECB/PKCS7 真实加解密服务。
     *
     * @return CryptoService 实现
     */
    @Bean
    public CryptoService cryptoService() {
        return new CryptoServiceImpl();
    }

    /**
     * SM4 主密钥 + SM2 登录密钥多版本加载服务。
     *
     * @param props    SM4 密钥配置
     * @param sm2Props SM2 登录密钥配置（GM S2a，可选段）
     * @return KeyService 实现
     */
    @Bean
    public KeyService keyService(final FepSecurityKeyProperties props,
                                 final FepSecuritySm2Properties sm2Props) {
        return new KeyServiceImpl(props, sm2Props);
    }
}
