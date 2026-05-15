package com.puchain.fep.processor.validation;

import com.puchain.fep.converter.type.MessageType;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * XSD-driven validation tests for the 3001 (outbound ProgressQuery) message body.
 *
 * <p>Coverage (P4-MSG-F T3，对齐 {@link CompanyInfoRequest1001XsdValidationTest} pattern):</p>
 * <ul>
 *     <li>Valid 3001 XML with all required + optional ExtInfo fields passes schema validation</li>
 *     <li>Valid 3001 XML with optional ExtInfo omitted still passes</li>
 *     <li>Invalid 3001 XML missing required {@code hxqyCode} is rejected</li>
 *     <li>{@link XsdSchemaRegistry} resolves {@link MessageType#MSG_3001}</li>
 * </ul>
 *
 * <p>DataType.xsd 实测约束（fixture 全部满足）: SerialNo(length=30) /
 * NodeCode(length=14) / OrgCode(length=14) / TransitionNo(Number length=8) /
 * qyName(minLen=2, maxLen=50) / qyCode(length=18 USCI) /
 * Number1to2(QueryType 1-2 位数字) / String0to100(QueryKey maxLen=100).</p>
 *
 * <p>3001.xsd RealHead3001 type=RequestHead (实测 line 31)；root element
 * {@code ProgressQuery3001} PascalCase (P4-Plan-C T1 实测)。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class ProgressQuery3001XsdValidationTest extends AbstractXsdValidationTest {

    private static final String VALID_FULL_FIELDS_XML = wrapCfx(
            "A1000142000001", "A1000143000104", "3001",
            "30010000000000000001", "00000000000000000000", """

                <RealHead3001>
                  <SendOrgCode>30500000000000</SendOrgCode>
                  <EntrustDate>20260513</EntrustDate>
                  <TransitionNo>00000001</TransitionNo>
                </RealHead3001>
                <ProgressQuery3001>
                  <SerialNo>SN300100000000000000000000001A</SerialNo>
                  <SendNodeCode>A1000142000001</SendNodeCode>
                  <DesNodeCode>A1000143000104</DesNodeCode>
                  <hxqyName>核心企业测试有限公司</hxqyName>
                  <hxqyCode>91110000ABCDEFGH12</hxqyCode>
                  <QueryType>1</QueryType>
                  <QueryKey>BIZ20260513000001</QueryKey>
                  <ExtInfo>
                    <ExtData>customExt</ExtData>
                  </ExtInfo>
                </ProgressQuery3001>
              """);

    private static final String VALID_OPTIONAL_OMITTED_XML = wrapCfx(
            "A1000142000001", "A1000143000104", "3001",
            "30010000000000000002", "00000000000000000000", """

                <RealHead3001>
                  <SendOrgCode>30500000000000</SendOrgCode>
                  <EntrustDate>20260513</EntrustDate>
                  <TransitionNo>00000002</TransitionNo>
                </RealHead3001>
                <ProgressQuery3001>
                  <SerialNo>SN300100000000000000000000002A</SerialNo>
                  <SendNodeCode>A1000142000001</SendNodeCode>
                  <DesNodeCode>A1000143000104</DesNodeCode>
                  <hxqyName>核心企业测试有限公司</hxqyName>
                  <hxqyCode>91110000ABCDEFGH12</hxqyCode>
                  <QueryType>1</QueryType>
                  <QueryKey>BIZ20260513000002</QueryKey>
                </ProgressQuery3001>
              """);

    private static final String INVALID_MISSING_HXQYCODE_XML = wrapCfx(
            "A1000142000001", "A1000143000104", "3001",
            "30010000000000000003", "00000000000000000000", """

                <RealHead3001>
                  <SendOrgCode>30500000000000</SendOrgCode>
                  <EntrustDate>20260513</EntrustDate>
                  <TransitionNo>00000003</TransitionNo>
                </RealHead3001>
                <ProgressQuery3001>
                  <SerialNo>SN300100000000000000000000003A</SerialNo>
                  <SendNodeCode>A1000142000001</SendNodeCode>
                  <DesNodeCode>A1000143000104</DesNodeCode>
                  <hxqyName>核心企业测试有限公司</hxqyName>
                  <QueryType>1</QueryType>
                  <QueryKey>BIZ20260513000003</QueryKey>
                </ProgressQuery3001>
              """);

    @Test
    void valid3001FullFields_shouldPass() {
        ValidationResult result = validator.validate(MessageType.MSG_3001,
                VALID_FULL_FIELDS_XML.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void valid3001OptionalOmitted_shouldPass() {
        ValidationResult result = validator.validate(MessageType.MSG_3001,
                VALID_OPTIONAL_OMITTED_XML.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void invalid3001_missingHxqyCode_shouldFail() {
        ValidationResult result = validator.validate(MessageType.MSG_3001,
                INVALID_MISSING_HXQYCODE_XML.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid()).isFalse();
        assertThat(result.errors())
                .as("error must reference missing hxqyCode field")
                .isNotEmpty()
                .anyMatch(e -> e.contains("hxqyCode"));
    }
}
