package com.puchain.fep.processor.validation;

import com.puchain.fep.converter.type.MessageType;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * XSD-driven validation tests for the 9100 (non-realtime universal forward)
 * message body.
 *
 * <p>Coverage (P4-MSG-I T3, 复用 {@link AbstractXsdValidationTest#wrapCfxTemplate}):
 * <ul>
 *     <li>Valid 9100 XML with all required + optional BusinessNo passes</li>
 *     <li>Invalid 9100 XML missing required body {@code Content} is rejected</li>
 * </ul>
 *
 * <p>9100.xsd body structure: {@code BatchHead9100 type=RequestHead}
 * (SendOrgCode/EntrustDate/TransitionNo required) + {@code Forward9100} body
 * (SrcNodeCode/SrcOrgCode/DesNodeCode/DesOrgCode/Content required, BusinessNo
 * optional minOccurs=0).</p>
 *
 * <p>DataType.xsd 实测约束（fixture 全部满足）: OrgCode/NodeCode(String length=14)
 * / Date(YYYYMMDD pattern) / TransitionNo(Number length=8) / SrcOrgCode/
 * DesOrgCode(String minLen=1 maxLen=14) / BusinessNo(Text minLen=1 maxLen=20) /
 * Content(Text unrestricted) / MsgId(Number length=20).</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class Forward9100XsdValidationTest extends AbstractXsdValidationTest {

    private static final String VALID_FULL_FIELDS_XML = wrapCfxTemplate(
            "A1000142000001", "A1000143000104", "FEPx", "9100",
            "91000000000000000001", "00000000000000000000", "20260519", """
                <BatchHead9100>
                  <SendOrgCode>30500000000000</SendOrgCode>
                  <EntrustDate>20260519</EntrustDate>
                  <TransitionNo>00000001</TransitionNo>
                </BatchHead9100>
                <Forward9100>
                  <SrcNodeCode>A1000142000001</SrcNodeCode>
                  <SrcOrgCode>30500000000000</SrcOrgCode>
                  <DesNodeCode>A1000143000104</DesNodeCode>
                  <DesOrgCode>30500000000099</DesOrgCode>
                  <BusinessNo>BUSINESS001</BusinessNo>
                  <Content>universal-forward-payload-9100</Content>
                </Forward9100>""");

    private static final String INVALID_MISSING_CONTENT_XML = wrapCfxTemplate(
            "A1000142000001", "A1000143000104", "FEPx", "9100",
            "91000000000000000002", "00000000000000000000", "20260519", """
                <BatchHead9100>
                  <SendOrgCode>30500000000000</SendOrgCode>
                  <EntrustDate>20260519</EntrustDate>
                  <TransitionNo>00000002</TransitionNo>
                </BatchHead9100>
                <Forward9100>
                  <SrcNodeCode>A1000142000001</SrcNodeCode>
                  <SrcOrgCode>30500000000000</SrcOrgCode>
                  <DesNodeCode>A1000143000104</DesNodeCode>
                  <DesOrgCode>30500000000099</DesOrgCode>
                </Forward9100>""");

    @Test
    void valid9100FullFields_shouldPass() {
        ValidationResult result = SHARED_VALIDATOR.validate(MessageType.MSG_9100,
                VALID_FULL_FIELDS_XML.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid())
                .as("9100 valid full fields errors=%s", result.errors())
                .isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void invalid9100_missingContent_shouldFail() {
        ValidationResult result = SHARED_VALIDATOR.validate(MessageType.MSG_9100,
                INVALID_MISSING_CONTENT_XML.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid()).isFalse();
        assertThat(result.errors())
                .as("error must reference missing required body Content field")
                .isNotEmpty()
                .anyMatch(e -> e.contains("Content"));
    }
}
