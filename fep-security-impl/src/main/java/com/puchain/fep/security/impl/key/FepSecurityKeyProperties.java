package com.puchain.fep.security.impl.key;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SM4 主密钥配置绑定（前缀 {@code fep.security.sm4}）。
 *
 * <p><strong>真实密钥永不入 repo:</strong> {@code sm4Keys} 的值（hex 编码 16 字节）在
 * 生产环境经 environment variable / sealed key store / envelope-encrypted 配置部署期注入
 * （如 application-prod.yml 用 {@code ${FEP_SM4_KEY_V1}}）；dev/CI 用 GB/T 测试密钥。</p>
 *
 * <p>多版本：{@code sm4Keys} 以 keyId → hex 密钥保存历史版本，支持轮换期共存；
 * {@code activeKeyId} 指向当前活跃版本（新加密用）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "fep.security.sm4")
public class FepSecurityKeyProperties {

    /** 当前活跃密钥版本号（新加密使用）。 */
    private String activeKeyId;

    /** keyId → hex 编码 16 字节 SM4 密钥（多版本共存）。 */
    private Map<String, String> sm4Keys = new LinkedHashMap<>();

    public String getActiveKeyId() {
        return activeKeyId;
    }

    public void setActiveKeyId(final String activeKeyId) {
        this.activeKeyId = activeKeyId;
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "Spring @ConfigurationProperties JavaBean binding mutates the map via "
                    + "this getter (relaxed binding); returning a defensive copy would break "
                    + "property binding. KeyServiceImpl copies the map at construction, so no live "
                    + "reference leaks beyond startup.")
    public Map<String, String> getSm4Keys() {
        return sm4Keys;
    }

    public void setSm4Keys(final Map<String, String> sm4Keys) {
        this.sm4Keys = sm4Keys == null ? new LinkedHashMap<>() : new LinkedHashMap<>(sm4Keys);
    }
}
