package com.puchain.fep.web.messageinbound.listener;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link InboundTransitionNoExtractor}.
 *
 * <p>R3 transitionNo 派生规范化升级：验证从业务头提取真实 TransitionNo，
 * 而非 msgId 末 8 位派生。关键反占位用例 {@link #extract_bodyTransitionNo_independentOfMsgId()}
 * 故意令 TransitionNo({@code 88888888}) ≠ MsgId 末 8 位({@code 00000111})。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class InboundTransitionNoExtractorTest {

    // 反占位证伪 fixture（TransitionNo=88888888 ≠ MsgId 末 8 位 00000111）抽取到
    // XsdTestSupport.INDEPENDENT_3115_XML（Rule-of-Three，2026-06-02 R3 Simplify Q-2）。

    private static final String NO_TRANSITION_NO_XML =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<CFX><HEAD><MsgNo>3116</MsgNo><MsgId>20260428000000000001</MsgId></HEAD>"
                    + "<MSG><BankCheckDay3116><SerialNo>SN20260428BANK</SerialNo></BankCheckDay3116></MSG></CFX>";

    private static final String BLANK_TRANSITION_NO_XML =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<CFX><HEAD><MsgNo>3115</MsgNo></HEAD>"
                    + "<MSG><BatchHead3115><TransitionNo>   </TransitionNo></BatchHead3115></MSG></CFX>";

    private static final String XXE_PROBE_XML =
            "<?xml version=\"1.0\"?>"
                    + "<!DOCTYPE foo [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]>"
                    + "<CFX><MSG><BatchHead3115><TransitionNo>&xxe;</TransitionNo></BatchHead3115></MSG></CFX>";

    @Test
    @DisplayName("业务头 TransitionNo 与 msgId 末 8 位独立 → 提取业务头值（反占位证伪）")
    void extract_bodyTransitionNo_independentOfMsgId() {
        assertThat(InboundTransitionNoExtractor.extract(XsdTestSupport.INDEPENDENT_3115_XML))
                .contains("88888888");
    }

    @Test
    @DisplayName("真实样本 3115-valid.xml → 提取 BatchHead 内 00000111")
    void extract_realSample3115_returnsBatchHeadValue() throws IOException {
        final String xml = readSample("samples/3115-valid.xml");
        assertThat(InboundTransitionNoExtractor.extract(xml)).contains("00000111");
    }

    @Test
    @DisplayName("MSG 无 TransitionNo 子元素 → empty")
    void extract_noTransitionNo_returnsEmpty() {
        assertThat(InboundTransitionNoExtractor.extract(NO_TRANSITION_NO_XML)).isEmpty();
    }

    @Test
    @DisplayName("TransitionNo 文本空白 → empty")
    void extract_blankTransitionNo_returnsEmpty() {
        assertThat(InboundTransitionNoExtractor.extract(BLANK_TRANSITION_NO_XML)).isEmpty();
    }

    @Test
    @DisplayName("合法但非 CFX 结构的 XML → empty")
    void extract_nonCfxWellFormedXml_returnsEmpty() {
        assertThat(InboundTransitionNoExtractor.extract("<not-cfx>broken</not-cfx>")).isEmpty();
    }

    @Test
    @DisplayName("真畸形 XML（未闭合标签）→ empty（解析异常被吞，不抛）")
    void extract_trulyMalformedXml_returnsEmpty() {
        assertThat(InboundTransitionNoExtractor.extract(
                "<CFX><MSG><BatchHead3115><TransitionNo>00000111")).isEmpty();
    }

    @Test
    @DisplayName("含 DOCTYPE 外部实体 → XXE-hardening 拒绝 → empty")
    void extract_xxePayload_rejectedReturnsEmpty() {
        assertThat(InboundTransitionNoExtractor.extract(XXE_PROBE_XML)).isEmpty();
    }

    private static String readSample(final String path) throws IOException {
        try (InputStream in = InboundTransitionNoExtractorTest.class
                .getClassLoader().getResourceAsStream(path)) {
            assertThat(in).as("test resource %s present", path).isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
