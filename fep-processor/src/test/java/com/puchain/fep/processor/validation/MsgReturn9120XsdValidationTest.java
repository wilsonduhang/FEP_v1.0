package com.puchain.fep.processor.validation;

import com.puchain.fep.converter.type.MessageType;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * XSD-driven validation tests for the 9120 (non-realtime universal ack) message.
 *
 * <p>Coverage (P4-MSG-I T3, 复用 {@link AbstractXsdValidationTest#wrapCfxTemplate}):
 * <ul>
 *     <li>Valid 9120 XML (required OriMsgNo + optional Debug) passes</li>
 *     <li>Invalid 9120 XML missing required body {@code OriMsgNo} is rejected</li>
 * </ul>
 *
 * <p>9120.xsd body structure (line 36): {@code BatchHead9120 type=ResponseHead}
 * (SendOrgCode/EntrustDate/TransitionNo/Result required, AddWord optional) +
 * {@code MsgReturn9120} body (OriMsgNo required = MsgNo length=4, Debug optional
 * = Text maxLen=1000).</p>
 *
 * <p>DataType.xsd 实测约束（fixture 全部满足）: OrgCode/NodeCode(String length=14)
 * / Date(YYYYMMDD pattern) / TransitionNo(Number length=8) / Result(Number
 * length=5) / AddWord(Text maxLen=200) / MsgNo(Number length=4) / Debug(Text
 * maxLen=1000) / MsgId(Number length=20) / App(String minLen=4, maxLen=20).</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class MsgReturn9120XsdValidationTest extends AbstractXsdValidationTest {

    /**
     * 9120-specific CFX envelope wrapper — 固定 SrcNode=A1000143000104 (HNDEMP) →
     * DesNode=A1000142000001 (FEP), App=HNDEMP, MsgNo=9120, WorkDate=20260519.
     *
     * @param msgIdSeq 20-digit MsgId (caller 提供, e.g. {@code "91200000000000000001"})
     * @param corrMsgIdSeq 20-digit CorrMsgId
     * @param msgInnerXml MSG 内层 XML（{@code BatchHead9120} + {@code MsgReturn9120}）
     * @return 完整 CFX envelope
     */
    private static String wrap(String msgIdSeq, String corrMsgIdSeq, String msgInnerXml) {
        return wrapCfxTemplate(
                "A1000143000104", "A1000142000001", "HNDEMP", "9120",
                msgIdSeq, corrMsgIdSeq, "20260519",
                msgInnerXml);
    }

    private static final String VALID_FULL_FIELDS_XML = wrap(
            "91200000000000000001", "30000000000000000001", """
                <BatchHead9120>
                  <SendOrgCode>30500000000000</SendOrgCode>
                  <EntrustDate>20260519</EntrustDate>
                  <TransitionNo>00000001</TransitionNo>
                  <Result>10000</Result>
                  <AddWord>处理成功</AddWord>
                </BatchHead9120>
                <MsgReturn9120>
                  <OriMsgNo>3000</OriMsgNo>
                  <Debug>processed at node A1000143000104</Debug>
                </MsgReturn9120>""");

    private static final String INVALID_MISSING_ORI_MSG_NO_XML = wrap(
            "91200000000000000002", "30000000000000000002", """
                <BatchHead9120>
                  <SendOrgCode>30500000000000</SendOrgCode>
                  <EntrustDate>20260519</EntrustDate>
                  <TransitionNo>00000002</TransitionNo>
                  <Result>10000</Result>
                </BatchHead9120>
                <MsgReturn9120>
                  <Debug>missing OriMsgNo on purpose</Debug>
                </MsgReturn9120>""");

    @Test
    void valid9120FullFields_shouldPass() {
        ValidationResult result = SHARED_VALIDATOR.validate(MessageType.MSG_9120,
                VALID_FULL_FIELDS_XML.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid())
                .as("9120 valid full fields errors=%s", result.errors())
                .isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void invalid9120_missingOriMsgNo_shouldFail() {
        ValidationResult result = SHARED_VALIDATOR.validate(MessageType.MSG_9120,
                INVALID_MISSING_ORI_MSG_NO_XML.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid()).isFalse();
        assertThat(result.errors())
                .as("error must reference missing required body OriMsgNo field")
                .isNotEmpty()
                .anyMatch(e -> e.contains("OriMsgNo"));
    }
}
