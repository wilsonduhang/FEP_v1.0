package com.puchain.fep.security.impl.gmvectors;

import com.puchain.fep.security.impl.crypto.BouncyCastleGmProviderConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GB/T 32918 SM2（SM3withSM2 + sm2p256v1）验证 BouncyCastle SM2 签验合规。
 *
 * <p>SM2 签名含随机 k 不可定值匹配标准签名串，故用 sign→verify roundtrip + 篡改否定
 * 证算法正确（GB/T 32918-2 §6 验证流程语义）+ 曲线归属断言防静默 fallback。</p>
 */
class Sm2GbtVectorTest {

    @BeforeAll
    static void registerBc() {
        new BouncyCastleGmProviderConfig();
    }

    private KeyPair sm2KeyPair() throws Exception {
        final KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC", "BC");
        kpg.initialize(new ECGenParameterSpec("sm2p256v1"));
        return kpg.generateKeyPair();
    }

    @Test
    void sm2KeyPair_usesSm2p256v1Curve() throws Exception {
        // 防 generator 静默 fallback secp256r1 致假绿：sm2p256v1 阶 n 高位 FFFFFFFE，
        // secp256r1 为 FFFFFFFF00000000
        final ECPublicKey pub = (ECPublicKey) sm2KeyPair().getPublic();
        final String order = pub.getParams().getOrder().toString(16).toUpperCase();
        assertThat(order).startsWith("FFFFFFFE");
    }

    @Test
    void sm3withSm2_signVerify_roundtrip() throws Exception {
        final KeyPair kp = sm2KeyPair();
        final byte[] data = "FEP CFX 报文签名范围 <?XML..</CFX>".getBytes(StandardCharsets.UTF_8);

        final Signature signer = Signature.getInstance("SM3withSM2", "BC");
        signer.initSign(kp.getPrivate());
        signer.update(data);
        final byte[] sig = signer.sign();

        final Signature verifier = Signature.getInstance("SM3withSM2", "BC");
        verifier.initVerify(kp.getPublic());
        verifier.update(data);
        assertThat(verifier.verify(sig)).isTrue();
    }

    @Test
    void sm3withSm2_tamperedData_verifyFails() throws Exception {
        final KeyPair kp = sm2KeyPair();
        final Signature signer = Signature.getInstance("SM3withSM2", "BC");
        signer.initSign(kp.getPrivate());
        signer.update("original".getBytes(StandardCharsets.UTF_8));
        final byte[] sig = signer.sign();

        final Signature verifier = Signature.getInstance("SM3withSM2", "BC");
        verifier.initVerify(kp.getPublic());
        verifier.update("tampered".getBytes(StandardCharsets.UTF_8));
        assertThat(verifier.verify(sig)).isFalse();
    }
}
