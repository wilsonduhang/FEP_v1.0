package com.puchain.fep.security.impl.crypto;

import jakarta.annotation.PostConstruct;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Security;

/**
 * 启动时注册 BouncyCastle GM provider（"BC"），为 SM2/SM3/SM4 JCE 调用奠基。
 *
 * <p>幂等：仅当 "BC" 未注册时添加，避免多次注册（测试/多 context）异常。
 * 国密实现（S1 CryptoServiceImpl / S2 SignServiceImpl）经 {@code getInstance("...", "BC")}
 * 解析 SM3/SM4/SM3withSM2 算法。</p>
 *
 * <p>非 Spring stereotype；经 {@code GmSecurityConfiguration} {@code @Bean}（gated impl）注册，
 * 或测试直接 {@code new} 实例化（构造即注册 BC）。</p>
 *
 * @since 1.0.0
 */
public class BouncyCastleGmProviderConfig {

    /**
     * 构造即尝试注册（供测试直接 new 验证 + 容器实例化时注册）。
     */
    public BouncyCastleGmProviderConfig() {
        registerBouncyCastle();
    }

    /**
     * Spring 容器启动后确保注册（幂等）。
     */
    @PostConstruct
    public void init() {
        registerBouncyCastle();
    }

    private static synchronized void registerBouncyCastle() {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }
}
