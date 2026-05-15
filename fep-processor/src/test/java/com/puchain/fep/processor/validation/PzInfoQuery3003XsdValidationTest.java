package com.puchain.fep.processor.validation;

import com.puchain.fep.converter.type.MessageType;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * XSD-driven validation tests for the 3003 (outbound pzInfoQuery) message body.
 *
 * <p>Coverage (P4-MSG-F T3，对齐 {@link CompanyInfoRequest1001XsdValidationTest} pattern):</p>
 * <ul>
 *     <li>Valid 3003 XML with all required + optional ExtInfo fields passes schema validation</li>
 *     <li>Valid 3003 XML with optional ExtInfo omitted still passes</li>
 *     <li>Invalid 3003 XML missing required {@code pzNo} is rejected</li>
 *     <li>{@link XsdSchemaRegistry} resolves {@link MessageType#MSG_3003}</li>
 * </ul>
 *
 * <p>DataType.xsd 实测约束: SerialNo(length=30) / NodeCode(length=14) / OrgCode(length=14) /
 * TransitionNo(Number length=8) / qyName(minLen=2, maxLen=50) / qyCode(length=18) /
 * pzNo(Text maxLen=100).</p>
 *
 * <p>3003.xsd RealHead3003 type=RequestHead (实测 line 31)；root element
 * {@code pzInfoQuery3003} <strong>camelCase</strong>（P4-Plan-C T1 实测 — XSD 混合命名）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class PzInfoQuery3003XsdValidationTest extends AbstractXsdValidationTest {

    private static final String VALID_FULL_FIELDS_XML = wrapCfx(
            "A1000142000001", "A1000143000104", "3003",
            "30030000000000000001", "00000000000000000000", """

                <RealHead3003>
                  <SendOrgCode>30500000000000</SendOrgCode>
                  <EntrustDate>20260513</EntrustDate>
                  <TransitionNo>00000001</TransitionNo>
                </RealHead3003>
                <pzInfoQuery3003>
                  <SerialNo>SN300300000000000000000000001A</SerialNo>
                  <SendNodeCode>A1000142000001</SendNodeCode>
                  <DesNodeCode>A1000143000104</DesNodeCode>
                  <hxqyName>核心企业测试有限公司</hxqyName>
                  <hxqyCode>91110000ABCDEFGH12</hxqyCode>
                  <pzNo>PZ20260513000000000001</pzNo>
                  <ExtInfo>
                    <ExtData>customExt</ExtData>
                  </ExtInfo>
                </pzInfoQuery3003>
              """);

    private static final String VALID_OPTIONAL_OMITTED_XML = wrapCfx(
            "A1000142000001", "A1000143000104", "3003",
            "30030000000000000002", "00000000000000000000", """

                <RealHead3003>
                  <SendOrgCode>30500000000000</SendOrgCode>
                  <EntrustDate>20260513</EntrustDate>
                  <TransitionNo>00000002</TransitionNo>
                </RealHead3003>
                <pzInfoQuery3003>
                  <SerialNo>SN300300000000000000000000002A</SerialNo>
                  <SendNodeCode>A1000142000001</SendNodeCode>
                  <DesNodeCode>A1000143000104</DesNodeCode>
                  <hxqyName>核心企业测试有限公司</hxqyName>
                  <hxqyCode>91110000ABCDEFGH12</hxqyCode>
                  <pzNo>PZ20260513000000000002</pzNo>
                </pzInfoQuery3003>
              """);

    private static final String INVALID_MISSING_PZNO_XML = wrapCfx(
            "A1000142000001", "A1000143000104", "3003",
            "30030000000000000003", "00000000000000000000", """

                <RealHead3003>
                  <SendOrgCode>30500000000000</SendOrgCode>
                  <EntrustDate>20260513</EntrustDate>
                  <TransitionNo>00000003</TransitionNo>
                </RealHead3003>
                <pzInfoQuery3003>
                  <SerialNo>SN300300000000000000000000003A</SerialNo>
                  <SendNodeCode>A1000142000001</SendNodeCode>
                  <DesNodeCode>A1000143000104</DesNodeCode>
                  <hxqyName>核心企业测试有限公司</hxqyName>
                  <hxqyCode>91110000ABCDEFGH12</hxqyCode>
                </pzInfoQuery3003>
              """);

    @Test
    void valid3003FullFields_shouldPass() {
        ValidationResult result = validator.validate(MessageType.MSG_3003,
                VALID_FULL_FIELDS_XML.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void valid3003OptionalOmitted_shouldPass() {
        ValidationResult result = validator.validate(MessageType.MSG_3003,
                VALID_OPTIONAL_OMITTED_XML.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void invalid3003_missingPzNo_shouldFail() {
        ValidationResult result = validator.validate(MessageType.MSG_3003,
                INVALID_MISSING_PZNO_XML.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid()).isFalse();
        assertThat(result.errors())
                .as("error must reference missing pzNo field")
                .isNotEmpty()
                .anyMatch(e -> e.contains("pzNo"));
    }
}
