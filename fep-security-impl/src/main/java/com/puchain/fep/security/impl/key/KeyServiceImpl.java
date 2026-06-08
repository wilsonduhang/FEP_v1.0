package com.puchain.fep.security.impl.key;

import com.puchain.fep.security.api.KeyService;
import jakarta.annotation.PostConstruct;

import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * SM4 主密钥多版本加载框架（PRD §3.3.4 密钥加载/版本/轮换）。
 *
 * <p>从 {@link FepSecurityKeyProperties} 加载 keyId → 16 字节 SM4 密钥的多版本映射，
 * 解码 hex 配置值（生产经 env/sealed store 注入，永不入 repo）。当前活跃版本由
 * {@code activeKeyId} 指向；历史版本供轮换期解密旧密文。</p>
 *
 * <p><strong>🔓 Mode A:</strong> 2026-06-07 muzhou 解禁国密域，AI 编写加载框架 + 密码学
 * 专项 review。真实密钥材料由密码设备生成、部署期注入，本类仅做加载/路由/校验。</p>
 *
 * <p><strong>S2 边界:</strong> SM2 相关方法（公钥分发/登录解密/签名私钥）抛
 * {@link UnsupportedOperationException}，属 S2 阶段（+ 架构 §0.3 决策门）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class KeyServiceImpl implements KeyService {

    /** SM4 密钥长度（字节）。 */
    private static final int SM4_KEY_LENGTH = 16;

    /** S2 边界提示。 */
    private static final String S2_PENDING =
            "SM2 key operations are pending S2 (see roadmap §0.3 sign-verify form decision)";

    private final String activeKeyId;
    private final Map<String, byte[]> keysByVersion;

    /**
     * 从配置构造，解码 hex 密钥为字节多版本映射。
     *
     * @param props SM4 密钥配置，非 null
     */
    public KeyServiceImpl(final FepSecurityKeyProperties props) {
        Objects.requireNonNull(props, "props");
        this.activeKeyId = props.getActiveKeyId();
        final Map<String, byte[]> decoded = new LinkedHashMap<>();
        props.getSm4Keys().forEach((keyId, hex) ->
                decoded.put(keyId, HexFormat.of().parseHex(hex)));
        this.keysByVersion = decoded;
    }

    /**
     * 启动期配置校验：activeKeyId 须存在于 keys map，每个密钥须 16 字节。
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
        throw new UnsupportedOperationException(S2_PENDING);
    }

    @Override
    public String decryptLoginPassword(final String encryptedBase64, final String keyId) {
        throw new UnsupportedOperationException(S2_PENDING);
    }

    @Override
    public byte[] getSignPrivateKey() {
        throw new UnsupportedOperationException(S2_PENDING);
    }
}
