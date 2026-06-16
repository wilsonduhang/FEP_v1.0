package com.puchain.fep.security.impl.key;

import com.puchain.fep.security.api.KeyService;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * KeyServiceImpl SM4 主密钥多版本加载 + 配置校验 + 防御性副本 + S2 边界。
 *
 * <p>测试密钥为 GB/T 测试向量 hex（公开值），非生产密钥。</p>
 */
class KeyServiceImplTest {

    private static final String GBT_KEY_HEX = "0123456789abcdeffedcba9876543210";
    private static final String OLD_KEY_HEX = "fedcba98765432100123456789abcdef";

    private static KeyServiceImpl newService(final String activeKeyId, final Map<String, String> keys) {
        final FepSecurityKeyProperties props = new FepSecurityKeyProperties();
        props.setActiveKeyId(activeKeyId);
        props.setSm4Keys(keys);
        final KeyServiceImpl svc = new KeyServiceImpl(props, new FepSecuritySm2Properties());
        svc.validateOnStartup();
        return svc;
    }

    private static KeyService newServiceWithSm2(final String keyId,
            final String privHex, final String pubHex) {
        return newServiceWithSm2Raw(keyId, keyId, privHex, pubHex);
    }

    private static KeyService newServiceWithSm2Raw(final String activeId, final String keyId,
            final String privHex, final String pubHex) {
        final FepSecuritySm2Properties sm2 = new FepSecuritySm2Properties();
        sm2.setLoginActiveKeyId(activeId);
        final FepSecuritySm2Properties.LoginKeyPair pair = new FepSecuritySm2Properties.LoginKeyPair();
        pair.setPrivateKeyHex(privHex);
        pair.setPublicKeyHex(pubHex);
        sm2.getLoginKeys().put(keyId, pair);
        final FepSecurityKeyProperties sm4 = new FepSecurityKeyProperties();
        sm4.setActiveKeyId("sm4-cred-v2");
        sm4.getSm4Keys().put("sm4-cred-v2", GBT_KEY_HEX);
        final KeyServiceImpl svc = new KeyServiceImpl(sm4, sm2);
        svc.validateOnStartup();
        return svc;
    }

    private static Map<String, String> keys(final String... kv) {
        final Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put(kv[i], kv[i + 1]);
        }
        return m;
    }

    @Test
    void getKeyId_returnsActiveKeyId() {
        final KeyService svc = newService("sm4-cred-v2", keys("sm4-cred-v2", GBT_KEY_HEX));
        assertThat(svc.getKeyId()).isEqualTo("sm4-cred-v2");
    }

    @Test
    void getActiveMasterKey_returnsSixteenBytes() {
        final KeyService svc = newService("sm4-cred-v2", keys("sm4-cred-v2", GBT_KEY_HEX));
        assertThat(svc.getSm4CredentialMasterKey()).hasSize(16);
    }

    @Test
    void getActiveMasterKey_returnsDefensiveCopy() {
        final KeyService svc = newService("sm4-cred-v2", keys("sm4-cred-v2", GBT_KEY_HEX));
        final byte[] first = svc.getSm4CredentialMasterKey();
        first[0] = (byte) 0xFF;
        assertThat(svc.getSm4CredentialMasterKey()[0]).isNotEqualTo((byte) 0xFF);
    }

    @Test
    void getMasterKeyByVersion_resolvesHistoricalKey() {
        final KeyService svc = newService("sm4-cred-v2",
                keys("sm4-cred-v2", GBT_KEY_HEX, "sm4-cred-v1", OLD_KEY_HEX));
        assertThat(svc.getSm4CredentialMasterKey("sm4-cred-v1")).hasSize(16);
        assertThat(svc.getSm4CredentialMasterKey("sm4-cred-v1"))
                .isNotEqualTo(svc.getSm4CredentialMasterKey("sm4-cred-v2"));
    }

    @Test
    void getMasterKeyByVersion_unknownKeyId_throws() {
        final KeyService svc = newService("sm4-cred-v2", keys("sm4-cred-v2", GBT_KEY_HEX));
        assertThatThrownBy(() -> svc.getSm4CredentialMasterKey("nonexistent"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void startup_activeKeyNotInMap_throws() {
        assertThatThrownBy(() -> newService("missing", keys("sm4-cred-v2", GBT_KEY_HEX)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void startup_keyNotSixteenBytes_throws() {
        assertThatThrownBy(() -> newService("bad", keys("bad", "00112233")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void sm2LoginAndMsgSignMethods_withoutConfig_throwIllegalState() {
        final KeyService svc = newService("sm4-cred-v2", keys("sm4-cred-v2", GBT_KEY_HEX));
        assertThatThrownBy(svc::getSm2PublicKeyBase64)
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("not configured");
        assertThatThrownBy(svc::getSm2LoginKeyId)
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> svc.decryptLoginPassword("00", "any"))
                .isInstanceOf(IllegalStateException.class);
        // S2b 实装：未配置 msg-sign 段时 getSignPrivateKey 抛 IllegalStateException
        // （不再是 UnsupportedOperationException）。
        assertThatThrownBy(svc::getSignPrivateKey)
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("msg-sign");
    }

    @Test
    void sm2LoginConfig_valid_exposesActiveKeyIdAndRawPointBase64() {
        final KeyService svc = newServiceWithSm2("sm2-login-v1",
                Sm2TestVectors.GBT_PRIVATE_KEY_HEX, Sm2TestVectors.GBT_PUBLIC_KEY_HEX);
        assertThat(svc.getSm2LoginKeyId()).isEqualTo("sm2-login-v1");
        final byte[] point = Base64.getDecoder().decode(svc.getSm2PublicKeyBase64());
        assertThat(point).hasSize(65);
        assertThat(point[0]).isEqualTo((byte) 0x04);
    }

    @Test
    void sm2LoginConfig_mismatchedKeyPair_failsStartup() {
        // 公钥末位 13 → 14（[d]G 配对校验必须抓住）
        final String tampered = Sm2TestVectors.GBT_PUBLIC_KEY_HEX
                .substring(0, Sm2TestVectors.GBT_PUBLIC_KEY_HEX.length() - 2) + "14";
        assertThatThrownBy(() -> newServiceWithSm2("sm2-login-v1",
                Sm2TestVectors.GBT_PRIVATE_KEY_HEX, tampered))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("key pair");
    }

    @Test
    void sm2LoginConfig_activeIdNotInMap_failsStartup() {
        assertThatThrownBy(() -> newServiceWithSm2Raw("missing-id", "sm2-login-v1",
                Sm2TestVectors.GBT_PRIVATE_KEY_HEX, Sm2TestVectors.GBT_PUBLIC_KEY_HEX))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void decryptLoginPassword_smCryptoFixture_recoversPassword() {
        final KeyService svc = newServiceWithSm2("sm2-login-v1",
                Sm2TestVectors.GBT_PRIVATE_KEY_HEX, Sm2TestVectors.GBT_PUBLIC_KEY_HEX);
        assertThat(svc.decryptLoginPassword(
                Sm2TestVectors.SM_CRYPTO_FIXTURE_CIPHER_HEX, "sm2-login-v1"))
                .isEqualTo(Sm2TestVectors.SM_CRYPTO_FIXTURE_PLAINTEXT);
    }

    @Test
    void decryptLoginPassword_unknownKeyId_throwsIllegalArgument() {
        final KeyService svc = newServiceWithSm2("sm2-login-v1",
                Sm2TestVectors.GBT_PRIVATE_KEY_HEX, Sm2TestVectors.GBT_PUBLIC_KEY_HEX);
        assertThatThrownBy(() -> svc.decryptLoginPassword(
                Sm2TestVectors.SM_CRYPTO_FIXTURE_CIPHER_HEX, "ghost-key"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sm2LoginConfig_privateKeyZeroScalar_failsStartup() {
        assertThatThrownBy(() -> newServiceWithSm2("sm2-login-v1",
                "00".repeat(32), Sm2TestVectors.GBT_PUBLIC_KEY_HEX))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("out of range");
    }

    @Test
    void sm2LoginConfig_privateKeyEqualToOrder_failsStartup() {
        // d = n（曲线阶，由 DOMAIN 实算而非字面值）→ 越上界（合法域 1..n-1）
        final String nHex = Sm2LoginCipher.DOMAIN.getN().toString(16);
        assertThatThrownBy(() -> newServiceWithSm2("sm2-login-v1",
                nHex, Sm2TestVectors.GBT_PUBLIC_KEY_HEX))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("out of range");
    }

    @Test
    void sm2LoginConfig_uppercaseHexKeyPair_passesValidation() {
        // 大写 hex 配置兼容（regex [0-9a-fA-F] + Hex.decode 双映射 + BigInteger(16) 大小写均解析）
        final KeyService svc = newServiceWithSm2("sm2-login-v1",
                Sm2TestVectors.GBT_PRIVATE_KEY_HEX.toUpperCase(Locale.ROOT),
                Sm2TestVectors.GBT_PUBLIC_KEY_HEX.toUpperCase(Locale.ROOT));
        assertThat(svc.getSm2LoginKeyId()).isEqualTo("sm2-login-v1");
    }

    @Test
    void sm2LoginConfig_publicKeyWithoutUncompressedPrefix_failsStartup() {
        // 02 压缩点前缀（长度合法 130 hex，前缀非 04）
        final String compressedPrefix = "02" + Sm2TestVectors.GBT_PUBLIC_KEY_HEX.substring(2);
        assertThatThrownBy(() -> newServiceWithSm2("sm2-login-v1",
                Sm2TestVectors.GBT_PRIVATE_KEY_HEX, compressedPrefix))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("uncompressed");
    }

    private static KeyService newServiceWithAudit(final String keyId,
            final String privHex, final String pubHex) {
        final FepSecuritySm2Properties sm2 = new FepSecuritySm2Properties();
        sm2.setAuditActiveKeyId(keyId);
        final FepSecuritySm2Properties.LoginKeyPair pair = new FepSecuritySm2Properties.LoginKeyPair();
        pair.setPrivateKeyHex(privHex);
        pair.setPublicKeyHex(pubHex);
        sm2.getAuditKeys().put(keyId, pair);
        final FepSecurityKeyProperties sm4 = new FepSecurityKeyProperties();
        sm4.setActiveKeyId("sm4-cred-v2");
        sm4.getSm4Keys().put("sm4-cred-v2", GBT_KEY_HEX);
        final KeyServiceImpl svc = new KeyServiceImpl(sm4, sm2);
        svc.validateOnStartup();
        return svc;
    }

    @Test
    void auditKeys_valid_exposeKeyIdPrivateBytesAndPublicHex() {
        final KeyService svc = newServiceWithAudit("sm2-audit-v1",
                Sm2TestVectors.GBT_PRIVATE_KEY_HEX, Sm2TestVectors.GBT_PUBLIC_KEY_HEX);
        assertThat(svc.getAuditKeyId()).isEqualTo("sm2-audit-v1");
        assertThat(svc.getAuditSignPrivateKey()).hasSize(32);
        assertThat(svc.getAuditVerifyPublicKeyHex("sm2-audit-v1"))
                .isEqualTo(Sm2TestVectors.GBT_PUBLIC_KEY_HEX);
    }

    @Test
    void auditSignPrivateKey_returnsDefensiveCopy() {
        final KeyService svc = newServiceWithAudit("sm2-audit-v1",
                Sm2TestVectors.GBT_PRIVATE_KEY_HEX, Sm2TestVectors.GBT_PUBLIC_KEY_HEX);
        final byte[] first = svc.getAuditSignPrivateKey();
        first[0] = (byte) 0xFF;
        assertThat(svc.getAuditSignPrivateKey()[0]).isNotEqualTo((byte) 0xFF);
    }

    @Test
    void auditKeys_withoutConfig_throwIllegalState_loginUnaffected() {
        final KeyService svc = newServiceWithSm2("sm2-login-v1",
                Sm2TestVectors.GBT_PRIVATE_KEY_HEX, Sm2TestVectors.GBT_PUBLIC_KEY_HEX);
        assertThatThrownBy(svc::getAuditSignPrivateKey)
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("not configured");
        assertThatThrownBy(svc::getAuditKeyId).isInstanceOf(IllegalStateException.class);
        assertThat(svc.getSm2LoginKeyId()).isEqualTo("sm2-login-v1");
    }

    @Test
    void auditKeys_mismatchedPair_failsStartup() {
        final String tampered = Sm2TestVectors.GBT_PUBLIC_KEY_HEX
                .substring(0, Sm2TestVectors.GBT_PUBLIC_KEY_HEX.length() - 2) + "14";
        assertThatThrownBy(() -> newServiceWithAudit("sm2-audit-v1",
                Sm2TestVectors.GBT_PRIVATE_KEY_HEX, tampered))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("key pair");
    }

    @Test
    void auditVerifyPublicKeyHex_unknownKeyId_throwsIllegalArgument() {
        final KeyService svc = newServiceWithAudit("sm2-audit-v1",
                Sm2TestVectors.GBT_PRIVATE_KEY_HEX, Sm2TestVectors.GBT_PUBLIC_KEY_HEX);
        assertThatThrownBy(() -> svc.getAuditVerifyPublicKeyHex("ghost"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ----- GM S2b: 报文签名私钥段（msg-sign）+ SrcNode 对端验签公钥段（peer-verify） -----

    private static KeyService newServiceWithMsgSign(final String keyId,
            final String privHex, final String pubHex) {
        return newServiceWithMsgSignRaw(keyId, keyId, privHex, pubHex);
    }

    private static KeyService newServiceWithMsgSignRaw(final String activeId, final String keyId,
            final String privHex, final String pubHex) {
        final FepSecuritySm2Properties sm2 = new FepSecuritySm2Properties();
        sm2.setMsgSignActiveKeyId(activeId);
        final FepSecuritySm2Properties.LoginKeyPair pair = new FepSecuritySm2Properties.LoginKeyPair();
        pair.setPrivateKeyHex(privHex);
        pair.setPublicKeyHex(pubHex);
        sm2.getMsgSignKeys().put(keyId, pair);
        final KeyServiceImpl svc = new KeyServiceImpl(sm4Props(), sm2);
        svc.validateOnStartup();
        return svc;
    }

    private static KeyService newServiceWithPeer(final String srcNode, final List<String> pubHexes) {
        final FepSecuritySm2Properties sm2 = new FepSecuritySm2Properties();
        sm2.getPeerVerifyKeys().put(srcNode, pubHexes);
        final KeyServiceImpl svc = new KeyServiceImpl(sm4Props(), sm2);
        svc.validateOnStartup();
        return svc;
    }

    private static FepSecurityKeyProperties sm4Props() {
        final FepSecurityKeyProperties sm4 = new FepSecurityKeyProperties();
        sm4.setActiveKeyId("sm4-cred-v2");
        sm4.getSm4Keys().put("sm4-cred-v2", GBT_KEY_HEX);
        return sm4;
    }

    @Test
    void msgSignConfigured_getSignPrivateKeyReturnsActiveScalar() {
        final KeyService svc = newServiceWithMsgSign("sm2-msgsign-v1",
                Sm2TestVectors.GBT_PRIVATE_KEY_HEX, Sm2TestVectors.GBT_PUBLIC_KEY_HEX);
        assertThat(svc.getSignPrivateKey())
                .hasSize(32)
                .isEqualTo(HexFormat.of().parseHex(Sm2TestVectors.GBT_PRIVATE_KEY_HEX));
    }

    @Test
    void getSignPrivateKey_returnsDefensiveCopy() {
        final KeyService svc = newServiceWithMsgSign("sm2-msgsign-v1",
                Sm2TestVectors.GBT_PRIVATE_KEY_HEX, Sm2TestVectors.GBT_PUBLIC_KEY_HEX);
        final byte[] first = svc.getSignPrivateKey();
        first[0] = (byte) 0xFF;
        assertThat(svc.getSignPrivateKey()[0]).isNotEqualTo((byte) 0xFF);
    }

    @Test
    void msgSignAbsent_getSignPrivateKeyThrowsIse() {
        final KeyService svc = newService("sm4-cred-v2", keys("sm4-cred-v2", GBT_KEY_HEX));
        assertThatThrownBy(svc::getSignPrivateKey)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("msg-sign");
    }

    @Test
    void msgSignActiveIdWithoutEntry_failsValidation() {
        assertThatThrownBy(() -> newServiceWithMsgSignRaw("sm2-msgsign-v9", "sm2-msgsign-v1",
                Sm2TestVectors.GBT_PRIVATE_KEY_HEX, Sm2TestVectors.GBT_PUBLIC_KEY_HEX))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void msgSignMismatchedKeyPair_failsStartup() {
        final String tampered = Sm2TestVectors.GBT_PUBLIC_KEY_HEX
                .substring(0, Sm2TestVectors.GBT_PUBLIC_KEY_HEX.length() - 2) + "14";
        assertThatThrownBy(() -> newServiceWithMsgSign("sm2-msgsign-v1",
                Sm2TestVectors.GBT_PRIVATE_KEY_HEX, tampered))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("key pair");
    }

    @Test
    void peerVerifyKeyValidPoint_passesStartup() {
        assertThatCode(() -> newServiceWithPeer("A1000143000104",
                List.of(Sm2TestVectors.GBT_PUBLIC_KEY_HEX)))
                .doesNotThrowAnyException();
    }

    @Test
    void peerVerifyKeyMultipleRotation_passesStartup() {
        // 换证期 list 含两个合法公钥（try-each 抗轮换）
        assertThatCode(() -> newServiceWithPeer("A1000143000104",
                List.of(Sm2TestVectors.GBT_PUBLIC_KEY_HEX, Sm2TestVectors.GBT_PUBLIC_KEY_HEX)))
                .doesNotThrowAnyException();
    }

    @Test
    void peerVerifyKeyBadFormat_failsStartup() {
        assertThatThrownBy(() -> newServiceWithPeer("A1000143000104", List.of("DEADBEEF")))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("peer");
    }

    @Test
    void peerVerifyKeyNotOnCurve_failsStartup() {
        // 130 hex 04 前缀格式合法，但 y 坐标被篡改 → 非曲线点（decodePoint 探活拒绝）
        final String notOnCurve = Sm2TestVectors.GBT_PUBLIC_KEY_HEX.substring(0, 128) + "0000";
        assertThatThrownBy(() -> newServiceWithPeer("A1000143000104", List.of(notOnCurve)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void peerVerifyKeyEmptyList_failsStartup() {
        assertThatThrownBy(() -> newServiceWithPeer("A1000143000104", List.of()))
                .isInstanceOf(IllegalStateException.class);
    }
}
