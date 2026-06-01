package com.puchain.fep.processor.validation;

import com.puchain.fep.converter.type.MessageType;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * XSD-driven validation tests for the 3004 (outbound pzInfoReturn) message body.
 *
 * <p>Coverage (P4-MSG-F T3，对齐 {@link CompanyInfoRequest1001XsdValidationTest} pattern):</p>
 * <ul>
 *     <li>Valid 3004 XML with all required + optional (ExtInfo) fields passes schema validation</li>
 *     <li>Valid 3004 XML with optional ExtInfo (and remaining optionals RiskRate/pzInfo/zpzAllInfo/edUpdateDateTime) omitted still passes</li>
 *     <li>Invalid 3004 XML missing required {@code pzState} is rejected</li>
 *     <li>{@link XsdSchemaRegistry} resolves {@link MessageType#MSG_3004}</li>
 * </ul>
 *
 * <p>DataType.xsd 实测约束: SerialNo(length=30) / NodeCode(length=14) / OrgCode(length=14) /
 * TransitionNo(Number length=8) / qyName(minLen=2, maxLen=50) / qyCode(length=18) /
 * pzNo(Text maxLen=100) / Number1to2(pzState/pzrzState) / Number2(rzPhaseCode) /
 * Result(Number length=5) / AddWord(Text maxLen=200).</p>
 *
 * <p>3004.xsd RealHead3004 type=ResponseHead (实测 line 31) — 含 Result (length=5) 必填 +
 * AddWord 可选；body root {@code pzInfoReturn3004} <strong>camelCase</strong> + body element
 * on MSG is {@code minOccurs="0"} (3004.xsd line 36)；body required 字段：SerialNo /
 * SendNodeCode / DesNodeCode / hxqyName / hxqyCode / pzNo / pzState / pzrzState /
 * pzrzStatusInfo（含 pzNo + rzPhaseCode + BankNodeCode）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class PzInfoReturn3004XsdValidationTest extends AbstractXsdValidationTest {

    private static final String VALID_FULL_FIELDS_XML = wrapCfxTemplate(
            HNDEMP_NODE, INSTITUTION_NODE, APP_FEPX, "3004",
            "30040000000000000001", "30030000000000000001", "20260513", """
                <RealHead3004>
                  <SendOrgCode>30500000000000</SendOrgCode>
                  <EntrustDate>20260513</EntrustDate>
                  <TransitionNo>00000001</TransitionNo>
                  <Result>10000</Result>
                  <AddWord>查询成功</AddWord>
                </RealHead3004>
                <pzInfoReturn3004>
                  <SerialNo>SN300400000000000000000000001A</SerialNo>
                  <SendNodeCode>A1000143000104</SendNodeCode>
                  <DesNodeCode>A1000142000001</DesNodeCode>
                  <hxqyName>核心企业测试有限公司</hxqyName>
                  <hxqyCode>91110000ABCDEFGH12</hxqyCode>
                  <pzNo>PZ20260513000000000001</pzNo>
                  <pzState>1</pzState>
                  <pzrzState>1</pzrzState>
                  <pzrzStatusInfo>
                    <pzNo>PZ20260513000000000001</pzNo>
                    <rzPhaseCode>01</rzPhaseCode>
                    <BankNodeCode>A1000142000001</BankNodeCode>
                  </pzrzStatusInfo>
                  <ExtInfo>
                    <ExtData>customExt</ExtData>
                  </ExtInfo>
                </pzInfoReturn3004>""");

    private static final String VALID_OPTIONAL_OMITTED_XML = wrapCfxTemplate(
            HNDEMP_NODE, INSTITUTION_NODE, APP_FEPX, "3004",
            "30040000000000000002", "30030000000000000002", "20260513", """
                <RealHead3004>
                  <SendOrgCode>30500000000000</SendOrgCode>
                  <EntrustDate>20260513</EntrustDate>
                  <TransitionNo>00000002</TransitionNo>
                  <Result>10000</Result>
                </RealHead3004>
                <pzInfoReturn3004>
                  <SerialNo>SN300400000000000000000000002A</SerialNo>
                  <SendNodeCode>A1000143000104</SendNodeCode>
                  <DesNodeCode>A1000142000001</DesNodeCode>
                  <hxqyName>核心企业测试有限公司</hxqyName>
                  <hxqyCode>91110000ABCDEFGH12</hxqyCode>
                  <pzNo>PZ20260513000000000002</pzNo>
                  <pzState>1</pzState>
                  <pzrzState>1</pzrzState>
                  <pzrzStatusInfo>
                    <pzNo>PZ20260513000000000002</pzNo>
                    <rzPhaseCode>01</rzPhaseCode>
                    <BankNodeCode>A1000142000001</BankNodeCode>
                  </pzrzStatusInfo>
                </pzInfoReturn3004>""");

    private static final String INVALID_MISSING_PZSTATE_XML = wrapCfxTemplate(
            HNDEMP_NODE, INSTITUTION_NODE, APP_FEPX, "3004",
            "30040000000000000003", "30030000000000000003", "20260513", """
                <RealHead3004>
                  <SendOrgCode>30500000000000</SendOrgCode>
                  <EntrustDate>20260513</EntrustDate>
                  <TransitionNo>00000003</TransitionNo>
                  <Result>10000</Result>
                </RealHead3004>
                <pzInfoReturn3004>
                  <SerialNo>SN300400000000000000000000003A</SerialNo>
                  <SendNodeCode>A1000143000104</SendNodeCode>
                  <DesNodeCode>A1000142000001</DesNodeCode>
                  <hxqyName>核心企业测试有限公司</hxqyName>
                  <hxqyCode>91110000ABCDEFGH12</hxqyCode>
                  <pzNo>PZ20260513000000000003</pzNo>
                  <pzrzState>1</pzrzState>
                  <pzrzStatusInfo>
                    <pzNo>PZ20260513000000000003</pzNo>
                    <rzPhaseCode>01</rzPhaseCode>
                    <BankNodeCode>A1000142000001</BankNodeCode>
                  </pzrzStatusInfo>
                </pzInfoReturn3004>""");

    @Test
    void valid3004FullFields_shouldPass() {
        ValidationResult result = validator.validate(MessageType.MSG_3004,
                VALID_FULL_FIELDS_XML.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void valid3004OptionalOmitted_shouldPass() {
        ValidationResult result = validator.validate(MessageType.MSG_3004,
                VALID_OPTIONAL_OMITTED_XML.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void invalid3004_missingPzState_shouldFail() {
        ValidationResult result = validator.validate(MessageType.MSG_3004,
                INVALID_MISSING_PZSTATE_XML.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid()).isFalse();
        assertThat(result.errors())
                .as("error must reference missing pzState field")
                .isNotEmpty()
                .anyMatch(e -> e.contains("pzState"));
    }
}
