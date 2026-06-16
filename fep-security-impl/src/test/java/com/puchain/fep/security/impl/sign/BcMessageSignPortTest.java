package com.puchain.fep.security.impl.sign;

import com.puchain.fep.security.impl.key.FepSecurityKeyProperties;
import com.puchain.fep.security.impl.key.FepSecuritySm2Properties;
import com.puchain.fep.security.impl.key.KeyServiceImpl;
import com.puchain.fep.security.impl.key.Sm2TestVectors;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * BcMessageSignPort B 形态签验委托测试（真 SignServiceImpl + 真 KeyServiceImpl，零 mock）。
 *
 * <p>密钥 = GB/T 32918.5-2017 附录 A 公开标准向量（非生产密钥）；roundtrip 互证复用 S5 已
 * 仲裁原语。try-each 抗轮换用 sm2p256v1 标准生成元 G 作"合法但非签名方"的对端公钥
 * （G 必在曲线上 → 启动校验通过；非 GBT 公钥 → 不验 GBT 签名）。</p>
 */
class BcMessageSignPortTest {

    private static final String SM4_KEY_HEX = "0123456789abcdeffedcba9876543210";
    private static final String SELF_NODE = "A1000143000104";
    private static final byte[] REPORT = "<CFX>report-body</CFX>".getBytes(StandardCharsets.UTF_8);

    /** sm2p256v1 标准生成元 G（04∥Gx∥Gy）= 合法曲线点、非 GBT 签名方公钥。 */
    private static final String SM2_GENERATOR_HEX =
            "04" + "32c4ae2c1f1981195f9904466a39c9948fe30bbff2660be1715a4589334c74c7"
                 + "bc3736a2f4f6779c59bdcee36b692153d0a9877cc62a474002df32e52139f0a0";

    private static BcMessageSignPort port(final boolean withMsgSign,
            final Map<String, List<String>> peers) {
        final FepSecuritySm2Properties sm2 = new FepSecuritySm2Properties();
        if (withMsgSign) {
            sm2.setMsgSignActiveKeyId("sm2-msgsign-v1");
            final FepSecuritySm2Properties.LoginKeyPair pair =
                    new FepSecuritySm2Properties.LoginKeyPair();
            pair.setPrivateKeyHex(Sm2TestVectors.GBT_PRIVATE_KEY_HEX);
            pair.setPublicKeyHex(Sm2TestVectors.GBT_PUBLIC_KEY_HEX);
            sm2.getMsgSignKeys().put("sm2-msgsign-v1", pair);
        }
        peers.forEach((srcNode, hexes) -> sm2.getPeerVerifyKeys().put(srcNode, hexes));
        final FepSecurityKeyProperties sm4 = new FepSecurityKeyProperties();
        sm4.setActiveKeyId("sm4-cred-v2");
        sm4.getSm4Keys().put("sm4-cred-v2", SM4_KEY_HEX);
        final KeyServiceImpl keyService = new KeyServiceImpl(sm4, sm2);
        keyService.validateOnStartup();
        return new BcMessageSignPort(new SignServiceImpl(), keyService, sm2);
    }

    @Test
    void signThenVerify_roundtripWithMatchingPeer() {
        final BcMessageSignPort port = port(true,
                Map.of(SELF_NODE, List.of(Sm2TestVectors.GBT_PUBLIC_KEY_HEX)));
        final String sig = port.sign(REPORT);
        assertThat(port.verify(REPORT, sig, SELF_NODE)).isTrue();
    }

    @Test
    void verify_tamperedSignature_returnsFalse() {
        final BcMessageSignPort port = port(true,
                Map.of(SELF_NODE, List.of(Sm2TestVectors.GBT_PUBLIC_KEY_HEX)));
        final String sig = port.sign(REPORT);
        final byte[] tampered = "<CFX>tampered</CFX>".getBytes(StandardCharsets.UTF_8);
        assertThat(port.verify(tampered, sig, SELF_NODE)).isFalse();
    }

    @Test
    void verify_unconfiguredSrcNode_throwsIse() {
        final BcMessageSignPort port = port(true,
                Map.of(SELF_NODE, List.of(Sm2TestVectors.GBT_PUBLIC_KEY_HEX)));
        final String sig = port.sign(REPORT);
        assertThatThrownBy(() -> port.verify(REPORT, sig, "UNKNOWN_NODE"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("UNKNOWN_NODE");
    }

    @Test
    void verify_tryEachRotation_secondKeyMatches() {
        // list = [G(非签名方), GBT_PUB(签名方)] → 第一个验失败、第二个验过 → true
        final BcMessageSignPort port = port(true,
                Map.of(SELF_NODE, List.of(SM2_GENERATOR_HEX, Sm2TestVectors.GBT_PUBLIC_KEY_HEX)));
        final String sig = port.sign(REPORT);
        assertThat(port.verify(REPORT, sig, SELF_NODE)).isTrue();
    }

    @Test
    void verify_allConfiguredKeysWrong_returnsFalse() {
        final BcMessageSignPort port = port(true, Map.of(SELF_NODE, List.of(SM2_GENERATOR_HEX)));
        final String sig = port.sign(REPORT);
        assertThat(port.verify(REPORT, sig, SELF_NODE)).isFalse();
    }

    @Test
    void sign_msgSignUnconfigured_throwsIse() {
        final BcMessageSignPort port = port(false,
                Map.of(SELF_NODE, List.of(Sm2TestVectors.GBT_PUBLIC_KEY_HEX)));
        assertThatThrownBy(() -> port.sign(REPORT))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("msg-sign");
    }
}
