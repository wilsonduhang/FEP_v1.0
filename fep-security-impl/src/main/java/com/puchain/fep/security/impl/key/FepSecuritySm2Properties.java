package com.puchain.fep.security.impl.key;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * SM2 登录密钥多版本配置（{@code fep.security.sm2.*}）。
 *
 * <p>真实密钥生产期经 env/sealed store 注入，永不入 repo；dev/CI 用 GB/T 32918.5-2017
 * 附录 A 公开标准测试密钥对。私钥为 32 字节标量 d 的 hex（64 字符），公钥为
 * 未压缩裸点 04∥x∥y 的 hex（130 字符）。配置整段可选——未配置时 SM2 登录方法
 * 抛 IllegalStateException（KeyServiceImpl）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "fep.security.sm2")
public class FepSecuritySm2Properties {

    /** 当前活跃登录密钥版本号（公钥分发与新登录解密使用）。 */
    private String loginActiveKeyId;

    /** keyId → 登录密钥对（多版本，轮换期共存）。 */
    private Map<String, LoginKeyPair> loginKeys = new LinkedHashMap<>();

    /**
     * 当前活跃登录密钥版本号。
     *
     * @return 活跃登录密钥版本号
     */
    public String getLoginActiveKeyId() {
        return loginActiveKeyId;
    }

    /**
     * 设置活跃登录密钥版本号。
     *
     * @param loginActiveKeyId 活跃登录密钥版本号
     */
    public void setLoginActiveKeyId(final String loginActiveKeyId) {
        this.loginActiveKeyId = loginActiveKeyId;
    }

    /**
     * 登录密钥多版本映射。Spring relaxed binding 需 live 引用填充（红线
     * feedback_configurationproperties_collection_getter_ei_expose）；
     * 下游 KeyServiceImpl 构造期拷贝，无 live 泄漏。
     *
     * @return keyId → 登录密钥对 map（live 引用）
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "Spring relaxed binding mutates via live getter; "
                    + "KeyServiceImpl copies on construction, no live reference escapes")
    public Map<String, LoginKeyPair> getLoginKeys() {
        return loginKeys;
    }

    /**
     * 设置登录密钥多版本映射（防御拷贝）。
     *
     * @param loginKeys keyId → 登录密钥对
     */
    public void setLoginKeys(final Map<String, LoginKeyPair> loginKeys) {
        this.loginKeys = new LinkedHashMap<>(loginKeys);
    }

    /**
     * 单版本 SM2 登录密钥对。
     */
    public static class LoginKeyPair {

        /** 私钥标量 d（hex 64 字符）。 */
        private String privateKeyHex;

        /** 公钥未压缩裸点 04∥x∥y（hex 130 字符）。 */
        private String publicKeyHex;

        /**
         * 私钥 hex。
         *
         * @return 私钥 hex
         */
        public String getPrivateKeyHex() {
            return privateKeyHex;
        }

        /**
         * 设置私钥 hex。
         *
         * @param privateKeyHex 私钥 hex
         */
        public void setPrivateKeyHex(final String privateKeyHex) {
            this.privateKeyHex = privateKeyHex;
        }

        /**
         * 公钥 hex。
         *
         * @return 公钥 hex
         */
        public String getPublicKeyHex() {
            return publicKeyHex;
        }

        /**
         * 设置公钥 hex。
         *
         * @param publicKeyHex 公钥 hex
         */
        public void setPublicKeyHex(final String publicKeyHex) {
            this.publicKeyHex = publicKeyHex;
        }
    }
}
