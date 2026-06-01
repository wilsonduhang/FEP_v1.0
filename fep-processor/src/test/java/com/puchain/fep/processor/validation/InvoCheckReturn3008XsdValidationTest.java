package com.puchain.fep.processor.validation;

import com.puchain.fep.converter.type.MessageType;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * XSD-driven validation tests for the 3008 (outbound InvoCheckReturn) message body.
 *
 * <p>Coverage (P4-MSG-G T4b，对齐 {@link ProgressQuery3001XsdValidationTest} pattern,
 * 复用 {@link AbstractXsdValidationTest#wrapCfxTemplate}):</p>
 * <ul>
 *     <li>Valid 3008 XML with all required + optional fields (head AddWord +
 *         body ExtInfo) passes schema validation</li>
 *     <li>Valid 3008 XML with optional head {@code AddWord} omitted still passes</li>
 *     <li>Invalid 3008 XML missing required head {@code Result} is rejected
 *         (3008.xsd RealHead3008 type=ResponseHead, Base.xsd:112 Result required)</li>
 *     <li>{@link XsdSchemaRegistry} resolves {@link MessageType#MSG_3008}</li>
 * </ul>
 *
 * <p>DataType.xsd 实测约束（fixture 全部满足）: SerialNo(Text length=30) /
 * NodeCode(String length=14) / OrgCode(String length=14) /
 * TransitionNo(Number length=8) / Result(Number length=5) /
 * AddWord(Text maxLen=200) / Number1to2(InvoCheckReturnCode 1-2 位数字) /
 * qyName(kpName/spName Text minLen=2, maxLen=50) /
 * qyCode(kpCode Text length=18).</p>
 *
 * <p>3008.xsd RealHead3008 type=ResponseHead (实测 line 31)；body root
 * {@code InvoCheckReturn3008} PascalCase + body element on MSG is
 * {@code minOccurs="0"} (3008.xsd line 36)。required body 字段：SerialNo /
 * SendNodeCode / DesNodeCode / InvoCheckReturnCode / kpName / kpCode / spName。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class InvoCheckReturn3008XsdValidationTest extends AbstractXsdValidationTest {

    private static final String VALID_FULL_FIELDS_XML = wrapCfxTemplate(
            HNDEMP_NODE, INSTITUTION_NODE, APP_FEPX, "3008",
            "30080000000000000001", "30070000000000000001", "20260514", """
                <RealHead3008>
                  <SendOrgCode>30500000000000</SendOrgCode>
                  <EntrustDate>20260514</EntrustDate>
                  <TransitionNo>00000001</TransitionNo>
                  <Result>10000</Result>
                  <AddWord>核验成功</AddWord>
                </RealHead3008>
                <InvoCheckReturn3008>
                  <SerialNo>SN300800000000000000000000001A</SerialNo>
                  <SendNodeCode>A1000143000104</SendNodeCode>
                  <DesNodeCode>A1000142000001</DesNodeCode>
                  <InvoCheckReturnCode>1</InvoCheckReturnCode>
                  <InvoCheckReturnMemo>核验通过</InvoCheckReturnMemo>
                  <kpName>销售方测试有限公司</kpName>
                  <kpCode>91110000ABCDEFGH12</kpCode>
                  <spName>购买方测试有限公司</spName>
                  <spCode>91110000ABCDEFGH34</spCode>
                  <ExtInfo>
                    <ExtData>customExt</ExtData>
                  </ExtInfo>
                </InvoCheckReturn3008>""");

    private static final String VALID_ADDWORD_OMITTED_XML = wrapCfxTemplate(
            HNDEMP_NODE, INSTITUTION_NODE, APP_FEPX, "3008",
            "30080000000000000002", "30070000000000000002", "20260514", """
                <RealHead3008>
                  <SendOrgCode>30500000000000</SendOrgCode>
                  <EntrustDate>20260514</EntrustDate>
                  <TransitionNo>00000002</TransitionNo>
                  <Result>10000</Result>
                </RealHead3008>
                <InvoCheckReturn3008>
                  <SerialNo>SN300800000000000000000000002A</SerialNo>
                  <SendNodeCode>A1000143000104</SendNodeCode>
                  <DesNodeCode>A1000142000001</DesNodeCode>
                  <InvoCheckReturnCode>1</InvoCheckReturnCode>
                  <kpName>销售方测试有限公司</kpName>
                  <kpCode>91110000ABCDEFGH12</kpCode>
                  <spName>购买方测试有限公司</spName>
                </InvoCheckReturn3008>""");

    private static final String INVALID_MISSING_RESULT_XML = wrapCfxTemplate(
            HNDEMP_NODE, INSTITUTION_NODE, APP_FEPX, "3008",
            "30080000000000000003", "30070000000000000003", "20260514", """
                <RealHead3008>
                  <SendOrgCode>30500000000000</SendOrgCode>
                  <EntrustDate>20260514</EntrustDate>
                  <TransitionNo>00000003</TransitionNo>
                </RealHead3008>
                <InvoCheckReturn3008>
                  <SerialNo>SN300800000000000000000000003A</SerialNo>
                  <SendNodeCode>A1000143000104</SendNodeCode>
                  <DesNodeCode>A1000142000001</DesNodeCode>
                  <InvoCheckReturnCode>1</InvoCheckReturnCode>
                  <kpName>销售方测试有限公司</kpName>
                  <kpCode>91110000ABCDEFGH12</kpCode>
                  <spName>购买方测试有限公司</spName>
                </InvoCheckReturn3008>""");

    @Test
    void valid3008FullFields_shouldPass() {
        ValidationResult result = validator.validate(MessageType.MSG_3008,
                VALID_FULL_FIELDS_XML.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid())
                .as("3008 valid full fields errors=%s", result.errors())
                .isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void valid3008AddWordOmitted_shouldPass() {
        ValidationResult result = validator.validate(MessageType.MSG_3008,
                VALID_ADDWORD_OMITTED_XML.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid())
                .as("3008 valid AddWord-omitted errors=%s", result.errors())
                .isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void invalid3008_missingResult_shouldFail() {
        ValidationResult result = validator.validate(MessageType.MSG_3008,
                INVALID_MISSING_RESULT_XML.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid()).isFalse();
        assertThat(result.errors())
                .as("error must reference missing required head Result field")
                .isNotEmpty()
                .anyMatch(e -> e.contains("Result"));
    }

    @Test
    void registrySupportsMsg3008() {
        assertThat(SHARED_REGISTRY.schemaOf(MessageType.MSG_3008)).isNotNull();
    }
}
