package com.puchain.fep.processor.validation;

import com.puchain.fep.converter.type.MessageType;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * XSD-driven validation tests for the 3103 (inbound ArchiveReturnInfo) message body.
 *
 * <p>Coverage (P4-MSG-G T4b，对齐 {@link ProgressQuery3001XsdValidationTest} pattern,
 * 复用 {@link AbstractXsdValidationTest#wrapCfxTemplate}):</p>
 * <ul>
 *     <li>Valid 3103 XML with all required + optional fields passes schema
 *         validation</li>
 *     <li>Valid 3103 XML with optional fields omitted (head AddWord + body
 *         CreationRetInfo/rzqyBankCusCode/.../ExtInfo) still passes</li>
 *     <li>Invalid 3103 XML missing required body {@code rzqyCode} is rejected</li>
 *     <li>{@link XsdSchemaRegistry} resolves {@link MessageType#MSG_3103}</li>
 * </ul>
 *
 * <p>DataType.xsd 实测约束（fixture 全部满足）: SerialNo(Text length=30) /
 * NodeCode(String length=14) / OrgCode(String length=14) /
 * TransitionNo(Number length=8) / Result(Number length=5) /
 * AddWord(Text maxLen=200) / Number1to2(CreationRetCode 1-2 位数字) /
 * qyName(hxqyName/rzqyName Text minLen=2, maxLen=50) /
 * qyCode(hxqyCode/rzqyCode Text length=18) /
 * String0to300(CreationRetInfo maxLen=300).</p>
 *
 * <p>3103.xsd BatchHead3103 type=ResponseHead (实测 line 31) — 含 Result
 * (length=5) 必填 + AddWord 可选；body root {@code ArchiveReturnInfo3103}
 * PascalCase + body element on MSG required (no minOccurs=0, 3103.xsd line 36)。
 * required body 字段：SerialNo / SendNodeCode / DesNodeCode / CreationRetCode /
 * hxqyName / hxqyCode / rzqyName / rzqyCode。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class ArchiveReturnInfo3103XsdValidationTest extends AbstractXsdValidationTest {

    private static final String VALID_FULL_FIELDS_XML = wrapCfxTemplate(
            "A1000143000104", "A1000142000001", "HNDEMP", "3103",
            "31030000000000000001", "31010000000000000001", "20260514", """
                <BatchHead3103>
                  <SendOrgCode>30500000000000</SendOrgCode>
                  <EntrustDate>20260514</EntrustDate>
                  <TransitionNo>00000001</TransitionNo>
                  <Result>10000</Result>
                  <AddWord>建档成功</AddWord>
                </BatchHead3103>
                <ArchiveReturnInfo3103>
                  <SerialNo>SN310300000000000000000000001A</SerialNo>
                  <SendNodeCode>A1000143000104</SendNodeCode>
                  <DesNodeCode>A1000142000001</DesNodeCode>
                  <CreationRetCode>1</CreationRetCode>
                  <CreationRetInfo>建档完成</CreationRetInfo>
                  <hxqyName>核心企业测试有限公司</hxqyName>
                  <hxqyCode>91110000ABCDEFGH12</hxqyCode>
                  <rzqyName>融资企业测试有限公司</rzqyName>
                  <rzqyCode>91110000ABCDEFGH34</rzqyCode>
                  <StartDate>20260101</StartDate>
                  <EndDate>20271231</EndDate>
                  <ExtInfo>
                    <ExtData>customExt</ExtData>
                  </ExtInfo>
                </ArchiveReturnInfo3103>""");

    private static final String VALID_OPTIONAL_OMITTED_XML = wrapCfxTemplate(
            "A1000143000104", "A1000142000001", "HNDEMP", "3103",
            "31030000000000000002", "31010000000000000002", "20260514", """
                <BatchHead3103>
                  <SendOrgCode>30500000000000</SendOrgCode>
                  <EntrustDate>20260514</EntrustDate>
                  <TransitionNo>00000002</TransitionNo>
                  <Result>10000</Result>
                </BatchHead3103>
                <ArchiveReturnInfo3103>
                  <SerialNo>SN310300000000000000000000002A</SerialNo>
                  <SendNodeCode>A1000143000104</SendNodeCode>
                  <DesNodeCode>A1000142000001</DesNodeCode>
                  <CreationRetCode>1</CreationRetCode>
                  <hxqyName>核心企业测试有限公司</hxqyName>
                  <hxqyCode>91110000ABCDEFGH12</hxqyCode>
                  <rzqyName>融资企业测试有限公司</rzqyName>
                  <rzqyCode>91110000ABCDEFGH34</rzqyCode>
                </ArchiveReturnInfo3103>""");

    private static final String INVALID_MISSING_RZQYCODE_XML = wrapCfxTemplate(
            "A1000143000104", "A1000142000001", "HNDEMP", "3103",
            "31030000000000000003", "31010000000000000003", "20260514", """
                <BatchHead3103>
                  <SendOrgCode>30500000000000</SendOrgCode>
                  <EntrustDate>20260514</EntrustDate>
                  <TransitionNo>00000003</TransitionNo>
                  <Result>10000</Result>
                </BatchHead3103>
                <ArchiveReturnInfo3103>
                  <SerialNo>SN310300000000000000000000003A</SerialNo>
                  <SendNodeCode>A1000143000104</SendNodeCode>
                  <DesNodeCode>A1000142000001</DesNodeCode>
                  <CreationRetCode>1</CreationRetCode>
                  <hxqyName>核心企业测试有限公司</hxqyName>
                  <hxqyCode>91110000ABCDEFGH12</hxqyCode>
                  <rzqyName>融资企业测试有限公司</rzqyName>
                </ArchiveReturnInfo3103>""");

    @Test
    void valid3103FullFields_shouldPass() {
        ValidationResult result = validator.validate(MessageType.MSG_3103,
                VALID_FULL_FIELDS_XML.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid())
                .as("3103 valid full fields errors=%s", result.errors())
                .isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void valid3103OptionalOmitted_shouldPass() {
        ValidationResult result = validator.validate(MessageType.MSG_3103,
                VALID_OPTIONAL_OMITTED_XML.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid())
                .as("3103 valid optional-omitted errors=%s", result.errors())
                .isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void invalid3103_missingRzqyCode_shouldFail() {
        ValidationResult result = validator.validate(MessageType.MSG_3103,
                INVALID_MISSING_RZQYCODE_XML.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid()).isFalse();
        assertThat(result.errors())
                .as("error must reference missing required body rzqyCode field")
                .isNotEmpty()
                .anyMatch(e -> e.contains("rzqyCode"));
    }
}
