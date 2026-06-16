package com.puchain.fep.security.impl;

import com.puchain.fep.security.api.CryptoService;
import com.puchain.fep.security.api.KeyService;
import com.puchain.fep.security.api.MessageSignPort;
import com.puchain.fep.security.api.SignService;
import com.puchain.fep.security.impl.crypto.BouncyCastleGmProviderConfig;
import com.puchain.fep.security.impl.crypto.CryptoServiceImpl;
import com.puchain.fep.security.impl.key.FepSecurityKeyProperties;
import com.puchain.fep.security.impl.key.FepSecuritySm2Properties;
import com.puchain.fep.security.impl.key.KeyServiceImpl;
import com.puchain.fep.security.impl.sign.BcMessageSignPort;
import com.puchain.fep.security.impl.sign.SignServiceImpl;
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
 * <p>impl 实现类（{@link CryptoServiceImpl}/{@link KeyServiceImpl}/{@link SignServiceImpl}/
 * {@link BouncyCastleGmProviderConfig}）
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

    /**
     * SM3withSM2 裸签真实服务（GM S5 审计行签名；S2b 报文签验 wiring 待 §0.3 定调）。
     *
     * @return SignService 实现
     */
    @Bean
    public SignService signService() {
        return new SignServiceImpl();
    }

    /**
     * 报文签验 port B 形态（进程内 BC；形态 C-ev，ADR 2026-06-12）。私钥经 KeyService 单源，
     * 按 SrcNode 路由 peer-verify-keys 验签。
     *
     * @param signService SM2 签验原语
     * @param keyService  报文签名私钥单源（getSignPrivateKey）
     * @param sm2Props    peer-verify-keys 配置
     * @return MessageSignPort 实现
     */
    @Bean
    public MessageSignPort messageSignPort(final SignService signService,
                                           final KeyService keyService,
                                           final FepSecuritySm2Properties sm2Props) {
        return new BcMessageSignPort(signService, keyService, sm2Props);
    }
}
