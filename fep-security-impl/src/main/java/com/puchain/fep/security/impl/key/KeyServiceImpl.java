package com.puchain.fep.security.impl.key;

import com.puchain.fep.security.api.KeyService;
import jakarta.annotation.PostConstruct;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * SM4 主密钥 + SM2 登录密钥多版本加载框架（PRD §3.3.4 密钥加载/版本/轮换）。
 *
 * <p>从 {@link FepSecurityKeyProperties} 加载 keyId → 16 字节 SM4 密钥的多版本映射；
 * 从 {@link FepSecuritySm2Properties} 加载 keyId → SM2 登录密钥对的多版本映射（GM S2a，
 * 可选配置——未配置时 SM2 登录方法抛 IllegalStateException）。hex 配置值生产经
 * env/sealed store 注入，永不入 repo。当前活跃版本分别由 {@code activeKeyId}（SM4）/
 * {@code loginActiveKeyId}（SM2 登录）指向，两命名空间独立轮换。</p>
 *
 * <p><strong>🔓 Mode A/B:</strong> 2026-06-07 muzhou 解禁国密域，AI 编写 + 密码学
 * 专项 review。真实密钥材料由密码设备生成、部署期注入，本类仅做加载/路由/校验/
 * 登录解密门面（SM2 原语在 {@link Sm2LoginCipher}）。</p>
 *
 * <p><strong>GM S2b（形态 C-ev，ADR 2026-06-12）:</strong> {@link #getSignPrivateKey()}
 * 返回当前活跃报文签名私钥（32 字节标量 d），消费方经 {@code MessageSignPort} 隔离形态依赖；
 * 形态 A（外部签名验签服务器 1818）下私钥驻留外部设备，本方法不适用。{@code peer-verify-keys}
 * 段（SrcNode → 对端验签公钥列表）在启动期校验曲线点合法性。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class KeyServiceImpl implements KeyService {

    /** SM4 密钥长度（字节）。 */
    private static final int SM4_KEY_LENGTH = 16;

    /** SM2 报文签名密钥未配置提示。 */
    private static final String MSG_SIGN_NOT_CONFIGURED =
            "SM2 message-sign keys not configured "
                    + "(fep.security.sm2.msg-sign-active-key-id / msg-sign-keys)";

    /** 未压缩裸点 hex 形态正则（04∥x∥y，130 字符）。 */
    private static final String UNCOMPRESSED_POINT_HEX = "04[0-9a-fA-F]{128}";

    /** SM2 登录密钥未配置提示。 */
    private static final String SM2_LOGIN_NOT_CONFIGURED =
            "SM2 login keys not configured (fep.security.sm2.login-active-key-id / login-keys)";

    /** SM2 审计密钥未配置提示。 */
    private static final String SM2_AUDIT_NOT_CONFIGURED =
            "SM2 audit keys not configured (fep.security.sm2.audit-active-key-id / audit-keys)";

    private final String activeKeyId;
    private final Map<String, byte[]> keysByVersion;
    private final String loginActiveKeyId;
    private final Map<String, FepSecuritySm2Properties.LoginKeyPair> loginKeys;
    private final String auditActiveKeyId;
    private final Map<String, FepSecuritySm2Properties.LoginKeyPair> auditKeys;
    private final String msgSignActiveKeyId;
    private final Map<String, FepSecuritySm2Properties.LoginKeyPair> msgSignKeys;
    private final Map<String, List<String>> peerVerifyKeys;

    /**
     * 从配置构造：SM4 hex 密钥解码 + SM2 登录/审计/报文签名密钥对拷贝 + 对端公钥深拷贝。
     *
     * @param props    SM4 密钥配置，非 null
     * @param sm2Props SM2 登录/审计/报文签名/对端公钥配置，非 null（各段内容可为空 = 未配置）
     */
    public KeyServiceImpl(final FepSecurityKeyProperties props,
                          final FepSecuritySm2Properties sm2Props) {
        Objects.requireNonNull(props, "props");
        Objects.requireNonNull(sm2Props, "sm2Props");
        this.activeKeyId = props.getActiveKeyId();
        final Map<String, byte[]> decoded = new LinkedHashMap<>();
        props.getSm4Keys().forEach((keyId, hex) ->
                decoded.put(keyId, HexFormat.of().parseHex(hex)));
        this.keysByVersion = decoded;
        this.loginActiveKeyId = sm2Props.getLoginActiveKeyId();
        this.loginKeys = new LinkedHashMap<>(sm2Props.getLoginKeys());
        this.auditActiveKeyId = sm2Props.getAuditActiveKeyId();
        this.auditKeys = new LinkedHashMap<>(sm2Props.getAuditKeys());
        this.msgSignActiveKeyId = sm2Props.getMsgSignActiveKeyId();
        this.msgSignKeys = new LinkedHashMap<>(sm2Props.getMsgSignKeys());
        this.peerVerifyKeys = PeerVerifyKeyMaps.immutableHexCopy(sm2Props.getPeerVerifyKeys());
    }

    /**
     * 启动期配置校验：activeKeyId 须存在于 keys map，每个密钥须 16 字节；
     * SM2 登录段（若配置）须 activeId 在 map、私钥 64 hex 且 1 ≤ d ≤ n-1、
     * 公钥 130 hex 未压缩裸点、[d]G 与公钥点配对一致。
     *
     * @throws IllegalStateException 配置非法
     */
    @PostConstruct
    public void validateOnStartup() {
        if (activeKeyId == null || !keysByVersion.containsKey(activeKeyId)) {
            throw new IllegalStateException(
                    "fep.security.sm4.active-key-id [" + activeKeyId + "] not present in sm4Keys");
        }
        keysByVersion.forEach((keyId, key) -> {
            if (key.length != SM4_KEY_LENGTH) {
                throw new IllegalStateException("SM4 key [" + keyId + "] must be "
                        + SM4_KEY_LENGTH + " bytes (hex 32 chars), got " + key.length);
            }
        });
        validateSm2KeySection("login", "fep.security.sm2.login-active-key-id",
                loginActiveKeyId, loginKeys);
        validateSm2KeySection("audit", "fep.security.sm2.audit-active-key-id",
                auditActiveKeyId, auditKeys);
        validateSm2KeySection("msg-sign", "fep.security.sm2.msg-sign-active-key-id",
                msgSignActiveKeyId, msgSignKeys);
        validatePeerVerifyKeys();
    }

    /**
     * 对端验签公钥段校验（GM S2b，密码学 MAJOR-1）：每个 SrcNode 至少一个公钥，
     * 每个 hex 须未压缩裸点形态（{@value #UNCOMPRESSED_POINT_HEX}）且为 sm2p256v1
     * 曲线合法点（decodePoint 探活）。坏值启动期 fail-fast，区别于运行期验签失败。
     *
     * @throws IllegalStateException 对端公钥配置非法
     */
    private void validatePeerVerifyKeys() {
        peerVerifyKeys.forEach((srcNode, hexes) -> {
            if (hexes.isEmpty()) {
                throw new IllegalStateException(
                        "peer-verify-keys [" + srcNode + "] has no public key configured");
            }
            hexes.forEach(hex -> {
                if (hex == null || !hex.matches(UNCOMPRESSED_POINT_HEX)) {
                    throw new IllegalStateException("peer public key for [" + srcNode
                            + "] must be 130 hex chars starting with 04 (uncompressed point)");
                }
                if (!Sm2LoginCipher.isValidCurvePoint(hex)) {
                    throw new IllegalStateException("peer public key for [" + srcNode
                            + "] is not a valid sm2p256v1 curve point");
                }
            });
        });
    }

    /**
     * SM2 密钥段校验（login/audit 完全对称——红线 mapper_helper_trim_consistency 精神）：
     * 段空 = 未配置跳过；否则 activeId 须在 map、私钥 64 hex 且 1 ≤ d ≤ n-1、
     * 公钥 130 hex 未压缩裸点、[d]G 与公钥点配对一致。
     *
     * @param sectionName   段名（入异常消息定位）
     * @param activeIdProp  activeId 配置键名（入异常消息）
     * @param sectionActive 段活跃版本号
     * @param sectionKeys   段密钥对 map
     * @throws IllegalStateException 配置非法
     */
    private static void validateSm2KeySection(final String sectionName, final String activeIdProp,
            final String sectionActive,
            final Map<String, FepSecuritySm2Properties.LoginKeyPair> sectionKeys) {
        if (sectionActive == null && sectionKeys.isEmpty()) {
            return;
        }
        if (sectionActive == null || !sectionKeys.containsKey(sectionActive)) {
            throw new IllegalStateException(activeIdProp + " ["
                    + sectionActive + "] not present in " + sectionName + " keys");
        }
        sectionKeys.forEach((keyId, pair) -> {
            final String priv = pair.getPrivateKeyHex();
            final String pub = pair.getPublicKeyHex();
            if (priv == null || !priv.matches("[0-9a-fA-F]{64}")) {
                throw new IllegalStateException("SM2 " + sectionName + " private key [" + keyId
                        + "] must be 64 hex chars (32-byte scalar)");
            }
            final BigInteger d = new BigInteger(priv, 16);
            if (d.signum() <= 0 || d.compareTo(Sm2LoginCipher.DOMAIN.getN()) >= 0) {
                throw new IllegalStateException("SM2 " + sectionName + " private key [" + keyId
                        + "] scalar out of range (require 1 <= d <= n-1)");
            }
            if (pub == null || !pub.matches(UNCOMPRESSED_POINT_HEX)) {
                throw new IllegalStateException("SM2 " + sectionName + " public key [" + keyId
                        + "] must be 130 hex chars starting with 04 (uncompressed point)");
            }
            if (!Sm2LoginCipher.isMatchingKeyPair(priv, pub)) {
                throw new IllegalStateException("SM2 " + sectionName + " key pair [" + keyId
                        + "] mismatch: [d]G does not equal configured public key");
            }
        });
    }

    @Override
    public String getKeyId() {
        return activeKeyId;
    }

    @Override
    public byte[] getSm4CredentialMasterKey() {
        return keysByVersion.get(activeKeyId).clone();
    }

    @Override
    public byte[] getSm4CredentialMasterKey(final String keyId) {
        final byte[] key = keysByVersion.get(keyId);
        if (key == null) {
            throw new IllegalArgumentException("unknown SM4 key version: " + keyId);
        }
        return key.clone();
    }

    @Override
    public String getSm2PublicKeyBase64() {
        final FepSecuritySm2Properties.LoginKeyPair active = loginKeys.get(requireLoginConfigured());
        return Base64.getEncoder()
                .encodeToString(HexFormat.of().parseHex(active.getPublicKeyHex()));
    }

    @Override
    public String getSm2LoginKeyId() {
        return requireLoginConfigured();
    }

    @Override
    public String decryptLoginPassword(final String encryptedPassword, final String keyId) {
        requireLoginConfigured();
        final FepSecuritySm2Properties.LoginKeyPair pair = loginKeys.get(keyId);
        if (pair == null) {
            throw new IllegalArgumentException("Unknown SM2 login keyId: " + keyId);
        }
        final byte[] plain = Sm2LoginCipher.decryptC1C3C2(encryptedPassword, pair.getPrivateKeyHex());
        return new String(plain, StandardCharsets.UTF_8);
    }

    /**
     * 当前活跃报文签名私钥（32 字节标量 d）。消费方经 {@code MessageSignPort}
     * （形态 C-ev，ADR 2026-06-12）隔离形态依赖；形态 A（外部签名验签服务器 1818）下
     * 私钥驻留外部设备、本方法不适用——见类级 §GM S2b 段。每次 {@code parseHex} 新建数组
     * 即防御副本，调用方持有的字节不与内部状态共享。
     *
     * @return 报文签名私钥字节（32 字节）
     * @throws IllegalStateException msg-sign 段未配置
     */
    @Override
    public byte[] getSignPrivateKey() {
        if (msgSignActiveKeyId == null || msgSignKeys.isEmpty()) {
            throw new IllegalStateException(MSG_SIGN_NOT_CONFIGURED);
        }
        final FepSecuritySm2Properties.LoginKeyPair active = msgSignKeys.get(msgSignActiveKeyId);
        // parseHex 每次新建数组 = 防御副本（私钥 32 字节标量 d）
        return HexFormat.of().parseHex(active.getPrivateKeyHex());
    }

    @Override
    public String getAuditKeyId() {
        return requireAuditConfigured();
    }

    @Override
    public byte[] getAuditSignPrivateKey() {
        final FepSecuritySm2Properties.LoginKeyPair active = auditKeys.get(requireAuditConfigured());
        // parseHex 每次新建数组 = 防御副本
        return HexFormat.of().parseHex(active.getPrivateKeyHex());
    }

    @Override
    public String getAuditVerifyPublicKeyHex(final String keyId) {
        requireAuditConfigured();
        final FepSecuritySm2Properties.LoginKeyPair pair = auditKeys.get(keyId);
        if (pair == null) {
            throw new IllegalArgumentException("Unknown SM2 audit keyId: " + keyId);
        }
        return pair.getPublicKeyHex();
    }

    private String requireLoginConfigured() {
        if (loginActiveKeyId == null || loginKeys.isEmpty()) {
            throw new IllegalStateException(SM2_LOGIN_NOT_CONFIGURED);
        }
        return loginActiveKeyId;
    }

    private String requireAuditConfigured() {
        if (auditActiveKeyId == null || auditKeys.isEmpty()) {
            throw new IllegalStateException(SM2_AUDIT_NOT_CONFIGURED);
        }
        return auditActiveKeyId;
    }
}
