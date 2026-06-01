package com.puchain.fep.processor.validation;

import com.puchain.fep.converter.type.MessageType;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * XSD-driven validation tests for the 3005 (outbound qyAccQuery) message body.
 *
 * <p>Coverage (P4-MSG-F T3，对齐 {@link CompanyInfoRequest1001XsdValidationTest} pattern):</p>
 * <ul>
 *     <li>Valid 3005 XML with all required + optional ExtInfo fields passes schema validation</li>
 *     <li>Valid 3005 XML with optional ExtInfo omitted still passes</li>
 *     <li>Invalid 3005 XML missing required {@code qyAccCode} is rejected</li>
 *     <li>{@link XsdSchemaRegistry} resolves {@link MessageType#MSG_3005}</li>
 * </ul>
 *
 * <p>DataType.xsd 实测约束: SerialNo(length=30) / NodeCode(length=14) / OrgCode(length=14) /
 * TransitionNo(Number length=8) / AccName(Text minLen=2, maxLen=50) /
 * AccNumber(Number minLen=10, maxLen=30).</p>
 *
 * <p>3005.xsd RealHead3005 type=RequestHead (实测 line 31)；root element
 * {@code qyAccQuery3005} <strong>camelCase</strong>（P4-Plan-C T1 实测 — XSD 混合命名）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class QyAccQuery3005XsdValidationTest extends AbstractXsdValidationTest {

    private static final String VALID_FULL_FIELDS_XML = wrapCfxTemplate(
            INSTITUTION_NODE, HNDEMP_NODE, APP_FEPX, "3005",
            "30050000000000000001", "00000000000000000000", "20260513", """
                <RealHead3005>
                  <SendOrgCode>30500000000000</SendOrgCode>
                  <EntrustDate>20260513</EntrustDate>
                  <TransitionNo>00000001</TransitionNo>
                </RealHead3005>
                <qyAccQuery3005>
                  <SerialNo>SN300500000000000000000000001A</SerialNo>
                  <SendNodeCode>A1000142000001</SendNodeCode>
                  <DesNodeCode>A1000143000104</DesNodeCode>
                  <qyAccName>测试企业账户</qyAccName>
                  <qyAccCode>6228480000000000</qyAccCode>
                  <ExtInfo>
                    <ExtData>customExt</ExtData>
                  </ExtInfo>
                </qyAccQuery3005>""");

    private static final String VALID_OPTIONAL_OMITTED_XML = wrapCfxTemplate(
            INSTITUTION_NODE, HNDEMP_NODE, APP_FEPX, "3005",
            "30050000000000000002", "00000000000000000000", "20260513", """
                <RealHead3005>
                  <SendOrgCode>30500000000000</SendOrgCode>
                  <EntrustDate>20260513</EntrustDate>
                  <TransitionNo>00000002</TransitionNo>
                </RealHead3005>
                <qyAccQuery3005>
                  <SerialNo>SN300500000000000000000000002A</SerialNo>
                  <SendNodeCode>A1000142000001</SendNodeCode>
                  <DesNodeCode>A1000143000104</DesNodeCode>
                  <qyAccName>测试企业账户</qyAccName>
                  <qyAccCode>6228480000000000</qyAccCode>
                </qyAccQuery3005>""");

    private static final String INVALID_MISSING_QYACCCODE_XML = wrapCfxTemplate(
            INSTITUTION_NODE, HNDEMP_NODE, APP_FEPX, "3005",
            "30050000000000000003", "00000000000000000000", "20260513", """
                <RealHead3005>
                  <SendOrgCode>30500000000000</SendOrgCode>
                  <EntrustDate>20260513</EntrustDate>
                  <TransitionNo>00000003</TransitionNo>
                </RealHead3005>
                <qyAccQuery3005>
                  <SerialNo>SN300500000000000000000000003A</SerialNo>
                  <SendNodeCode>A1000142000001</SendNodeCode>
                  <DesNodeCode>A1000143000104</DesNodeCode>
                  <qyAccName>测试企业账户</qyAccName>
                </qyAccQuery3005>""");

    @Test
    void valid3005FullFields_shouldPass() {
        ValidationResult result = validator.validate(MessageType.MSG_3005,
                VALID_FULL_FIELDS_XML.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void valid3005OptionalOmitted_shouldPass() {
        ValidationResult result = validator.validate(MessageType.MSG_3005,
                VALID_OPTIONAL_OMITTED_XML.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void invalid3005_missingQyAccCode_shouldFail() {
        ValidationResult result = validator.validate(MessageType.MSG_3005,
                INVALID_MISSING_QYACCCODE_XML.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid()).isFalse();
        assertThat(result.errors())
                .as("error must reference missing qyAccCode field")
                .isNotEmpty()
                .anyMatch(e -> e.contains("qyAccCode"));
    }
}
