package com.puchain.fep.security.impl.key;

import com.puchain.fep.security.api.KeyService;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * KeyServiceImpl SM4 主密钥多版本加载 + 配置校验 + 防御性副本 + S2 边界。
 *
 * <p>测试密钥为 GB/T 测试向量 hex（公开值），非生产密钥。</p>
 */
class KeyServiceImplTest {

    private static final String GBT_KEY_HEX = "0123456789abcdeffedcba9876543210";
    private static final String OLD_KEY_HEX = "fedcba98765432100123456789abcdef";

    private KeyServiceImpl newService(final String activeKeyId, final Map<String, String> keys) {
        final FepSecurityKeyProperties props = new FepSecurityKeyProperties();
        props.setActiveKeyId(activeKeyId);
        props.setSm4Keys(keys);
        final KeyServiceImpl svc = new KeyServiceImpl(props);
        svc.validateOnStartup();
        return svc;
    }

    private Map<String, String> keys(final String... kv) {
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
    void sm2Methods_throwUnsupportedForS2Boundary() {
        final KeyService svc = newService("sm4-cred-v2", keys("sm4-cred-v2", GBT_KEY_HEX));
        assertThatThrownBy(svc::getSm2PublicKeyBase64)
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> svc.decryptLoginPassword("x", "y"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(svc::getSignPrivateKey)
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
