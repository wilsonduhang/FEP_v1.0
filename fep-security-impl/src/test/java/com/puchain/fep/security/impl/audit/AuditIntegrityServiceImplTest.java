package com.puchain.fep.security.impl.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.puchain.fep.security.api.AuditIntegrityService;
import com.puchain.fep.security.api.KeyService;
import com.puchain.fep.security.impl.hash.HashServiceImpl;
import com.puchain.fep.security.impl.key.FepSecurityKeyProperties;
import com.puchain.fep.security.impl.key.FepSecuritySm2Properties;
import com.puchain.fep.security.impl.key.KeyServiceImpl;
import com.puchain.fep.security.impl.key.Sm2TestVectors;
import com.puchain.fep.security.impl.sign.SignServiceImpl;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * 审计完整性原语：hash 链构造（SM3 直算对照锚定）+ 行签名 roundtrip（真 SignServiceImpl）。
 *
 * <p>密钥 = GB/T 32918.5-2017 附录 A 公开标准测试密钥对（非生产密钥）。</p>
 */
class AuditIntegrityServiceImplTest {

    private static final HashServiceImpl HASH = new HashServiceImpl();

    private static AuditIntegrityService service() {
        final FepSecuritySm2Properties sm2 = new FepSecuritySm2Properties();
        sm2.setAuditActiveKeyId("sm2-audit-v1");
        final FepSecuritySm2Properties.LoginKeyPair pair = new FepSecuritySm2Properties.LoginKeyPair();
        pair.setPrivateKeyHex(Sm2TestVectors.GBT_PRIVATE_KEY_HEX);
        pair.setPublicKeyHex(Sm2TestVectors.GBT_PUBLIC_KEY_HEX);
        sm2.getAuditKeys().put("sm2-audit-v1", pair);
        final FepSecurityKeyProperties sm4 = new FepSecurityKeyProperties();
        sm4.setActiveKeyId("sm4-cred-v1");
        sm4.getSm4Keys().put("sm4-cred-v1", "0123456789abcdeffedcba9876543210");
        final KeyServiceImpl keyService = new KeyServiceImpl(sm4, sm2);
        keyService.validateOnStartup();
        return new AuditIntegrityServiceImpl(HASH, new SignServiceImpl(), keyService);
    }

    @Test
    void genesisPrevHash_is64Zeros() {
        assertThat(AuditIntegrityService.GENESIS_PREV_HASH)
                .hasSize(64).matches("0{64}");
    }

    @Test
    void computeEntryHash_matchesDirectSm3OverConcatenation() {
        // 防实现自说自话：与 HashService 直算 SM3(prevHex_utf8 || canonical) 对照
        final String prev = AuditIntegrityService.GENESIS_PREV_HASH;
        final byte[] canonical = "abc".getBytes(StandardCharsets.UTF_8);
        final String expected = HASH.sm3Hex(
                (prev + "abc").getBytes(StandardCharsets.UTF_8));
        assertThat(service().computeEntryHash(prev, canonical)).isEqualTo(expected);
    }

    @Test
    void computeEntryHash_utf8CanonicalAndNonGenesisPrev_matchesDirectSm3() {
        final String prev = HASH.sm3Hex("seed".getBytes(StandardCharsets.UTF_8));
        final String canonicalText = "14:审计行✓2026";
        final byte[] canonical = canonicalText.getBytes(StandardCharsets.UTF_8);
        final String expected = HASH.sm3Hex(
                (prev + canonicalText).getBytes(StandardCharsets.UTF_8));
        assertThat(service().computeEntryHash(prev, canonical)).isEqualTo(expected);
    }

    @Test
    void signThenVerifyEntry_roundtrip_andKeyIdExposed() {
        final AuditIntegrityService svc = service();
        final String hash = svc.computeEntryHash(AuditIntegrityService.GENESIS_PREV_HASH,
                "row-1".getBytes(StandardCharsets.UTF_8));
        final String signature = svc.signEntryHash(hash);
        assertThat(svc.auditKeyId()).isEqualTo("sm2-audit-v1");
        assertThat(svc.verifyEntry(hash, signature, "sm2-audit-v1")).isTrue();
    }

    @Test
    void verifyEntry_tamperedHash_returnsFalse() {
        final AuditIntegrityService svc = service();
        final String hash = svc.computeEntryHash(AuditIntegrityService.GENESIS_PREV_HASH,
                "row-1".getBytes(StandardCharsets.UTF_8));
        final String signature = svc.signEntryHash(hash);
        final String tampered = (hash.charAt(0) == 'a' ? "b" : "a") + hash.substring(1);
        assertThat(svc.verifyEntry(tampered, signature, "sm2-audit-v1")).isFalse();
    }

    @Test
    void verifyEntry_unknownKeyId_throwsIllegalArgument() {
        final AuditIntegrityService svc = service();
        final String hash = svc.computeEntryHash(AuditIntegrityService.GENESIS_PREV_HASH,
                "row-1".getBytes(StandardCharsets.UTF_8));
        assertThatThrownBy(() -> svc.verifyEntry(hash, svc.signEntryHash(hash), "ghost"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void malformedInputs_throwIllegalArgument() {
        final AuditIntegrityService svc = service();
        assertThatThrownBy(() -> svc.computeEntryHash(null, new byte[0]))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> svc.computeEntryHash("zz", new byte[0]))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> svc.computeEntryHash(
                AuditIntegrityService.GENESIS_PREV_HASH, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> svc.signEntryHash(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> svc.verifyEntry(null, "sig", "k"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> svc.verifyEntry("0".repeat(64), null, "k"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
