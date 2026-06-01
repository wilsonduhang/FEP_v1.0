package com.puchain.fep.processor.validation;

import com.puchain.fep.converter.type.MessageType;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * XSD-driven validation tests for the 9000 (realtime universal forward) message
 * body.
 *
 * <p>Coverage (P4-MSG-I T3, 复用 {@link AbstractXsdValidationTest#wrapCfxTemplate}):
 * <ul>
 *     <li>Valid 9000 XML with all required + optional BusinessNo passes</li>
 *     <li>Invalid 9000 XML missing required body {@code DesOrgCode} is rejected</li>
 * </ul>
 *
 * <p>9000.xsd body structure: {@code RealHead9000 type=RequestHead}
 * (SendOrgCode/EntrustDate/TransitionNo required) + {@code Forward9000} body
 * (SrcNodeCode/SrcOrgCode/DesNodeCode/DesOrgCode/Content required, BusinessNo
 * optional minOccurs=0).</p>
 *
 * <p>DataType.xsd 实测约束（fixture 全部满足）: OrgCode/NodeCode(String length=14)
 * / Date(YYYYMMDD pattern) / TransitionNo(Number length=8) / SrcOrgCode/
 * DesOrgCode(String minLen=1 maxLen=14) / BusinessNo(Text minLen=1 maxLen=20) /
 * Content(Text unrestricted) / MsgId(Number length=20).</p>
 *
 * <p>9000 与 9100 的区别仅在业务头标签：9000 = {@code RealHead9000}（实时通用转发）
 * vs 9100 = {@code BatchHead9100}（非实时通用转发）；body Forward9000 / Forward9100
 * 字段集相同。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class Forward9000XsdValidationTest extends AbstractXsdValidationTest {

    /**
     * 9000-specific CFX envelope wrapper — 固定 SrcNode=A1000142000001 (FEP) →
     * DesNode=A1000143000104 (HNDEMP), App=FEPx, MsgNo=9000, WorkDate=20260519,
     * CorrMsgId 全 0 (9000 是 FEP-initiated 转发，无 correlation).
     *
     * @param msgIdSeq 20-digit MsgId (caller 提供, e.g. {@code "90000000000000000001"})
     * @param msgInnerXml MSG 内层 XML（{@code RealHead9000} + {@code Forward9000}）
     * @return 完整 CFX envelope
     */
    private static String wrap(String msgIdSeq, String msgInnerXml) {
        return wrapCfxTemplate(
                "A1000142000001", "A1000143000104", "FEPx", "9000",
                msgIdSeq, "00000000000000000000", "20260519",
                msgInnerXml);
    }

    private static final String VALID_FULL_FIELDS_XML = wrap(
            "90000000000000000001", """
                <RealHead9000>
                  <SendOrgCode>30500000000000</SendOrgCode>
                  <EntrustDate>20260519</EntrustDate>
                  <TransitionNo>00000001</TransitionNo>
                </RealHead9000>
                <Forward9000>
                  <SrcNodeCode>A1000142000001</SrcNodeCode>
                  <SrcOrgCode>30500000000000</SrcOrgCode>
                  <DesNodeCode>A1000143000104</DesNodeCode>
                  <DesOrgCode>30500000000099</DesOrgCode>
                  <BusinessNo>BUSINESS001</BusinessNo>
                  <Content>realtime-forward-payload-9000</Content>
                </Forward9000>""");

    private static final String INVALID_MISSING_DES_ORG_CODE_XML = wrap(
            "90000000000000000002", """
                <RealHead9000>
                  <SendOrgCode>30500000000000</SendOrgCode>
                  <EntrustDate>20260519</EntrustDate>
                  <TransitionNo>00000002</TransitionNo>
                </RealHead9000>
                <Forward9000>
                  <SrcNodeCode>A1000142000001</SrcNodeCode>
                  <SrcOrgCode>30500000000000</SrcOrgCode>
                  <DesNodeCode>A1000143000104</DesNodeCode>
                  <Content>realtime-forward-payload-9000-missing-desorg</Content>
                </Forward9000>""");

    @Test
    void valid9000FullFields_shouldPass() {
        ValidationResult result = SHARED_VALIDATOR.validate(MessageType.MSG_9000,
                VALID_FULL_FIELDS_XML.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid())
                .as("9000 valid full fields errors=%s", result.errors())
                .isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void invalid9000_missingDesOrgCode_shouldFail() {
        ValidationResult result = SHARED_VALIDATOR.validate(MessageType.MSG_9000,
                INVALID_MISSING_DES_ORG_CODE_XML.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid()).isFalse();
        assertThat(result.errors())
                .as("error must reference missing required body DesOrgCode field")
                .isNotEmpty()
                .anyMatch(e -> e.contains("DesOrgCode"));
    }
}
