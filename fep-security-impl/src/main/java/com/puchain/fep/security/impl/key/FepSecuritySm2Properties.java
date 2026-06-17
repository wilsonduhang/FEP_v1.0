package com.puchain.fep.security.impl.key;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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

    /** 当前活跃审计签名密钥版本号（GM S5 审计 hash 链行签名使用）。 */
    private String auditActiveKeyId;

    /** keyId → 审计签名密钥对（多版本，轮换期历史行验签）。 */
    private Map<String, LoginKeyPair> auditKeys = new LinkedHashMap<>();

    /** 当前活跃报文签名密钥版本号（GM S2b 出站报文 SM2 加签使用）。 */
    private String msgSignActiveKeyId;

    /** keyId → 报文签名密钥对（多版本，本节点报文签名身份轮换期共存）。 */
    private Map<String, LoginKeyPair> msgSignKeys = new LinkedHashMap<>();

    /**
     * SrcNode 节点代码 → 对端验签公钥 hex 列表（GM S2b 入站报文按发起方路由公钥；
     * list 抗轮换——HNDEMP 换证期新旧公钥共存 try-each，PRD §3.3.3 步骤 1）。
     */
    private Map<String, List<String>> peerVerifyKeys = new LinkedHashMap<>();

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
     * 设置登录密钥多版本映射（防御拷贝 + null guard，与 setSm4Keys 对称——S2a Simplify R5）。
     *
     * @param loginKeys keyId → 登录密钥对
     */
    public void setLoginKeys(final Map<String, LoginKeyPair> loginKeys) {
        this.loginKeys = copyOrEmpty(loginKeys);
    }

    /**
     * 当前活跃审计签名密钥版本号。
     *
     * @return 活跃审计密钥版本号
     */
    public String getAuditActiveKeyId() {
        return auditActiveKeyId;
    }

    /**
     * 设置活跃审计签名密钥版本号。
     *
     * @param auditActiveKeyId 活跃审计密钥版本号
     */
    public void setAuditActiveKeyId(final String auditActiveKeyId) {
        this.auditActiveKeyId = auditActiveKeyId;
    }

    /**
     * 审计密钥多版本映射。Spring relaxed binding 需 live 引用填充（红线
     * feedback_configurationproperties_collection_getter_ei_expose）；
     * 下游 KeyServiceImpl 构造期拷贝，无 live 泄漏。
     *
     * @return keyId → 审计密钥对 map（live 引用）
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "Spring relaxed binding mutates via live getter; "
                    + "KeyServiceImpl copies on construction, no live reference escapes")
    public Map<String, LoginKeyPair> getAuditKeys() {
        return auditKeys;
    }

    /**
     * 设置审计密钥多版本映射（防御拷贝 + null guard，与 setSm4Keys 对称）。
     *
     * @param auditKeys keyId → 审计密钥对
     */
    public void setAuditKeys(final Map<String, LoginKeyPair> auditKeys) {
        this.auditKeys = copyOrEmpty(auditKeys);
    }

    /**
     * 当前活跃报文签名密钥版本号。
     *
     * @return 活跃报文签名密钥版本号
     */
    public String getMsgSignActiveKeyId() {
        return msgSignActiveKeyId;
    }

    /**
     * 设置活跃报文签名密钥版本号。
     *
     * @param msgSignActiveKeyId 活跃报文签名密钥版本号
     */
    public void setMsgSignActiveKeyId(final String msgSignActiveKeyId) {
        this.msgSignActiveKeyId = msgSignActiveKeyId;
    }

    /**
     * 报文签名密钥多版本映射。Spring relaxed binding 需 live 引用填充（红线
     * feedback_configurationproperties_collection_getter_ei_expose）；
     * 下游 KeyServiceImpl 构造期拷贝，无 live 泄漏。
     *
     * @return keyId → 报文签名密钥对 map（live 引用）
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "Spring relaxed binding mutates via live getter; "
                    + "KeyServiceImpl copies on construction, no live reference escapes")
    public Map<String, LoginKeyPair> getMsgSignKeys() {
        return msgSignKeys;
    }

    /**
     * 设置报文签名密钥多版本映射（防御拷贝 + null guard，与 setLoginKeys 对称）。
     *
     * @param msgSignKeys keyId → 报文签名密钥对
     */
    public void setMsgSignKeys(final Map<String, LoginKeyPair> msgSignKeys) {
        this.msgSignKeys = copyOrEmpty(msgSignKeys);
    }

    /**
     * SrcNode → 对端验签公钥 hex 列表。Spring relaxed binding 需 live 引用填充（红线
     * feedback_configurationproperties_collection_getter_ei_expose）；
     * 下游 KeyServiceImpl/BcMessageSignPort 构造期深拷贝，无 live 泄漏。
     *
     * @return SrcNode → 公钥 hex 列表 map（live 引用）
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "Spring relaxed binding mutates via live getter; "
                    + "consumers deep-copy on construction, no live reference escapes")
    public Map<String, List<String>> getPeerVerifyKeys() {
        return peerVerifyKeys;
    }

    /**
     * 设置 SrcNode → 对端验签公钥列表映射（深拷贝 + null guard，对称防御）。
     *
     * @param peerVerifyKeys SrcNode → 公钥 hex 列表
     */
    public void setPeerVerifyKeys(final Map<String, List<String>> peerVerifyKeys) {
        final Map<String, List<String>> copy = new LinkedHashMap<>();
        if (peerVerifyKeys != null) {
            peerVerifyKeys.forEach((srcNode, hexes) ->
                    copy.put(srcNode, hexes == null ? new ArrayList<>() : new ArrayList<>(hexes)));
        }
        this.peerVerifyKeys = copy;
    }

    /**
     * Map 防御拷贝 + null guard 归一（REUSE-2）：login/audit/msg-sign 三段 setter 共用。
     * 返回<strong>可变</strong> LinkedHashMap——Spring relaxed binding 经 getter 取 live
     * 引用逐 key 填充，故不可返回不可变副本。{@code setPeerVerifyKeys} 是两层深拷贝
     * （内层 List 也拷），语义不同，不在本归一范围。
     *
     * @param source 入参 map（可 null）
     * @param <V>    值类型
     * @return source 的可变浅拷贝；source 为 null 时返回新空 map
     */
    private static <V> Map<String, V> copyOrEmpty(final Map<String, V> source) {
        return source == null ? new LinkedHashMap<>() : new LinkedHashMap<>(source);
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
