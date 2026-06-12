package com.puchain.fep.web.messageinbound.listener;

import com.puchain.fep.common.util.FepConstants;

/**
 * Shared XSD-related test fixture helpers for inbound wire integration tests.
 *
 * <p>Currently exposes {@link #pad30(String)} — padding/truncating raw business identifiers
 * to satisfy the {@code DataType.xsd} {@code SerialNo simpleType} {@code <xsd:length value="30"/>}
 * fixed-length constraint (派生 Text base, lines 616-623 in {@code DataType.xsd}). Required for
 * {@code SerialNo} fields across inbound listener tests when 真 {@code XsdValidator}
 * is wired (mock removed during R-NEW-1 真 XsdValidator 5 测试改造闭环, commits
 * {@code c8585e2} Inbound3112WireTest + {@code 04e8041} InboundAck9120BatchWireTest).
 *
 * <p>Extracted under Rule-of-2 (2 byte-identical copies in sibling test classes) on
 * 2026-05-28 to avoid a 3rd copy when future inbound wire tests (Inbound3105/3009/3103/3113
 * mirror-expansion) reproduce the same fixed-length SerialNo requirement.
 */
final class XsdTestSupport {

    private XsdTestSupport() {
        // utility class, no instantiation
    }

    /**
     * R3 反占位证伪 fixture：3115 报文，业务头 {@code BatchHead3115.TransitionNo = 88888888}
     * 故意 ≠ {@code MsgId} 末 8 位 {@code 00000111}，用于验证 transitionNo 取业务头真值而非
     * msgId 末 8 位派生。
     *
     * <p>Rule-of-Three 抽取（2026-06-02 R3 Simplify Q-2）：原在
     * {@code InboundTransitionNoExtractorTest}（1 处）+ {@code TlqInboundListenerTest}（1 处）
     * byte-identical 重复，且前者曾硬编码 SrcNode；统一引用 {@link FepConstants#HNDEMP_NODE_CODE}。</p>
     */
    static final String INDEPENDENT_3115_XML =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<CFX>"
                    + "<HEAD>"
                    + "<Version>1.0</Version>"
                    + "<SrcNode>" + FepConstants.HNDEMP_NODE_CODE + "</SrcNode>"
                    + "<DesNode>B43010104B0001</DesNode>"
                    + "<App>HNDEMP</App>"
                    + "<MsgNo>3115</MsgNo>"
                    + "<MsgId>20260424105000000111</MsgId>"
                    + "<CorrMsgId></CorrMsgId>"
                    + "<WorkDate>20260424</WorkDate>"
                    + "</HEAD>"
                    + "<MSG>"
                    + "<BatchHead3115>"
                    + "<SendOrgCode>A1000143000104</SendOrgCode>"
                    + "<EntrustDate>20260424</EntrustDate>"
                    + "<TransitionNo>88888888</TransitionNo>"
                    + "<Result>90000</Result>"
                    + "</BatchHead3115>"
                    + "<PlatPay3115>"
                    + "<SerialNo>SN2026042410500000000000000111</SerialNo>"
                    + "</PlatPay3115>"
                    + "</MSG>"
                    + "</CFX>";

    /**
     * Pads the input to exactly 30 characters by appending {@code '0'} on the right;
     * truncates to 30 characters if the input is longer. Satisfies the
     * {@code DataType.xsd} {@code SerialNo simpleType} fixed length=30 constraint
     * (lines 616-623).
     *
     * @param raw raw input string (never {@code null}; behavior on {@code null} is to throw NPE
     *            via {@code .length()} which is acceptable for test fixtures)
     * @return a 30-character string, never {@code null}
     */
    public static String pad30(final String raw) {
        final int pad = 30 - raw.length();
        if (pad <= 0) {
            return raw.substring(0, 30);
        }
        return raw + "0".repeat(pad);
    }
}
