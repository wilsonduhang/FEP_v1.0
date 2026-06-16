package com.puchain.fep.web.outbound.consumer;

import static org.assertj.core.api.Assertions.assertThat;

import com.puchain.fep.converter.sign.MessageVerifier;
import com.puchain.fep.web.support.Sm2WebTestVectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * GM S2b T4 — 出站加签真 wire 形态 + 真验签 roundtrip（provider=impl 全 context，真 BC 国密）。
 *
 * <p>证明 G1 修复：{@link OutboundSignAdapter#embedSignatureAsComment} 产物注释置于
 * {@code </CFX>} <strong>之后</strong>、格式 {@code <!--B64-->}，且可被 {@link MessageVerifier}
 * 按 SrcNode 路由对端公钥真验签通过（自签自验，msgsign 与 peer 同 GB/T 附录 A 密钥对）。</p>
 *
 * <p>密钥 = GB/T 32918.5-2017 附录 A 公开标准向量（非生产密钥）。命名 *Test 入 Surefire。</p>
 */
@SpringBootTest
@ActiveProfiles("dev")
@TestPropertySource(properties = {
        "fep.security.provider=impl",
        "fep.security.sm4.active-key-id=sm4-cred-v1",
        "fep.security.sm4.sm4-keys.sm4-cred-v1=0123456789abcdeffedcba9876543210",
        "fep.security.sm2.msg-sign-active-key-id=sm2-msgsign-v1",
        "fep.security.sm2.msg-sign-keys.sm2-msgsign-v1.private-key-hex=" + Sm2WebTestVectors.GBT_PRIV,
        "fep.security.sm2.msg-sign-keys.sm2-msgsign-v1.public-key-hex=" + Sm2WebTestVectors.GBT_PUB,
        "fep.security.sm2.peer-verify-keys.A1000143000104[0]=" + Sm2WebTestVectors.GBT_PUB
})
class OutboundSignWireRoundtripImplTest {

    private static final String HNDEMP_NODE = "A1000143000104";

    @Autowired
    private OutboundSignAdapter outboundSignAdapter;

    @Autowired
    private MessageVerifier messageVerifier;

    @Test
    void sign_producesCommentAfterClosingCfx_andVerifiesAgainstPeerKey() {
        final String xml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?><CFX><HEAD/><MSG/></CFX>";

        final String signed = outboundSignAdapter.embedSignatureAsComment(xml);

        // G1 修复：注释置 </CFX> 之后、格式 <!--B64-->（旧缺陷格式不再出现）
        assertThat(signed).contains("</CFX><!--");
        assertThat(signed).doesNotContain("<!-- signature:");
        // 真验签 roundtrip：按 SrcNode 路由对端公钥（自签自验同密钥对）
        assertThat(messageVerifier.verify(signed, HNDEMP_NODE)).isTrue();
    }

    @Test
    void verify_tamperedBody_returnsFalse() {
        final String xml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?><CFX><HEAD/><MSG/></CFX>";
        final String signed = outboundSignAdapter.embedSignatureAsComment(xml);
        // 篡改签名范围内的正文（注释前），验签必失败
        final String tampered = signed.replace("<MSG/>", "<MSG>X</MSG>");
        assertThat(messageVerifier.verify(tampered, HNDEMP_NODE)).isFalse();
    }
}
