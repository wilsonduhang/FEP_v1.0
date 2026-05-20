package com.puchain.fep.processor.validation;

import com.puchain.fep.converter.type.MessageType;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * XSD-driven validation tests for the 3113 (core enterprise credit query
 * response) message body.
 *
 * <p>Coverage (P4-MSG-I T3, 复用 {@link AbstractXsdValidationTest#wrapCfxTemplate}):
 * <ul>
 *     <li>Valid 3113 XML with all required head + body fields passes</li>
 *     <li>Valid 3113 XML with body element omitted (3113.xsd line 36
 *         {@code minOccurs=0}) still passes</li>
 *     <li>Invalid 3113 XML missing required body {@code SerialNo} is rejected</li>
 * </ul>
 *
 * <p><strong>关键 lowercase body tag 陷阱</strong>: 3113.xsd line 36
 * {@code <xsd:element name="hxqyCreditAmt3113" type="hxqyCreditAmt3113"
 * minOccurs="0">} 实测为 <strong>lowercase h</strong>，与 Java class
 * {@code HxqyCreditAmt3113} PascalCase <strong>不一致</strong>（类比 3108
 * lowercase p 先例）。fixture body element 标签必须用
 * {@code <hxqyCreditAmt3113>} 否则 XSD 拒绝。</p>
 *
 * <p>DataType.xsd 实测约束（fixture 全部满足）: OrgCode/NodeCode(String length=14)
 * / Date(YYYYMMDD pattern) / TransitionNo(Number length=8) / Result(Number
 * length=5) / SerialNo(Text length=30) / qyName(Text minLen=2 maxLen=50) /
 * qyCode(Text length=18) / Result(Number length=5 — 内部 RetCode 复用) /
 * String0to100(RetMemo maxLen=100) / Integer(CreditInfoNum xsd:integer) /
 * Currency(sxAmt/sxBalance 金额 pattern) / DateTime(QueryReturnTime
 * YYYYMMDDhhmmss).</p>
 *
 * <p>3113.xsd BatchHead3113 type=ResponseHead — 含 Result (length=5) 必填 +
 * AddWord 可选；body element on MSG <strong>minOccurs=0</strong> (3113.xsd line
 * 36, 与 3108 不同)。required body 字段：SerialNo / SendNodeCode / DesNodeCode /
 * QueryDate / CreditInfoNum / CreditInfo (maxOccurs=200)；子 CreditInfo required：
 * hxqyName / hxqyCode / RetCode。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class HxqyCreditAmt3113XsdValidationTest extends AbstractXsdValidationTest {

    private static final String VALID_FULL_FIELDS_XML = wrapCfxTemplate(
            "A1000143000104", "A1000142000001", "HNDEMP", "3113",
            "31130000000000000001", "31120000000000000001", "20260519", """
                <BatchHead3113>
                  <SendOrgCode>30500000000000</SendOrgCode>
                  <EntrustDate>20260519</EntrustDate>
                  <TransitionNo>00000001</TransitionNo>
                  <Result>10000</Result>
                  <AddWord>查询成功</AddWord>
                </BatchHead3113>
                <hxqyCreditAmt3113>
                  <SerialNo>SN311300000000000000000000001A</SerialNo>
                  <SendNodeCode>A1000143000104</SendNodeCode>
                  <DesNodeCode>A1000142000001</DesNodeCode>
                  <QueryDate>20260519</QueryDate>
                  <CreditInfoNum>1</CreditInfoNum>
                  <CreditInfo>
                    <hxqyName>核心企业测试有限公司</hxqyName>
                    <hxqyCode>91110000ABCDEFGH12</hxqyCode>
                    <RetCode>00000</RetCode>
                    <RetMemo>授信信息可用</RetMemo>
                    <CreditInfoMx>
                      <BankNodeCode>B1000142000001</BankNodeCode>
                      <BankName>测试银行总行</BankName>
                      <sxAmt>1000000.00</sxAmt>
                      <sxBalance>500000.00</sxBalance>
                      <QueryReturnTime>20260519101530</QueryReturnTime>
                    </CreditInfoMx>
                  </CreditInfo>
                  <ExtInfo>
                    <ExtData>customExt3113</ExtData>
                  </ExtInfo>
                </hxqyCreditAmt3113>""");

    private static final String VALID_BODY_OMITTED_XML = wrapCfxTemplate(
            "A1000143000104", "A1000142000001", "HNDEMP", "3113",
            "31130000000000000002", "31120000000000000002", "20260519", """
                <BatchHead3113>
                  <SendOrgCode>30500000000000</SendOrgCode>
                  <EntrustDate>20260519</EntrustDate>
                  <TransitionNo>00000002</TransitionNo>
                  <Result>20000</Result>
                  <AddWord>无授信信息</AddWord>
                </BatchHead3113>""");

    private static final String INVALID_MISSING_SERIAL_NO_XML = wrapCfxTemplate(
            "A1000143000104", "A1000142000001", "HNDEMP", "3113",
            "31130000000000000003", "31120000000000000003", "20260519", """
                <BatchHead3113>
                  <SendOrgCode>30500000000000</SendOrgCode>
                  <EntrustDate>20260519</EntrustDate>
                  <TransitionNo>00000003</TransitionNo>
                  <Result>10000</Result>
                </BatchHead3113>
                <hxqyCreditAmt3113>
                  <SendNodeCode>A1000143000104</SendNodeCode>
                  <DesNodeCode>A1000142000001</DesNodeCode>
                  <QueryDate>20260519</QueryDate>
                  <CreditInfoNum>1</CreditInfoNum>
                  <CreditInfo>
                    <hxqyName>核心企业测试有限公司</hxqyName>
                    <hxqyCode>91110000ABCDEFGH12</hxqyCode>
                    <RetCode>00000</RetCode>
                  </CreditInfo>
                </hxqyCreditAmt3113>""");

    @Test
    void valid3113FullFields_shouldPass() {
        ValidationResult result = SHARED_VALIDATOR.validate(MessageType.MSG_3113,
                VALID_FULL_FIELDS_XML.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid())
                .as("3113 valid full fields errors=%s", result.errors())
                .isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void valid3113BodyOmitted_shouldPass() {
        ValidationResult result = SHARED_VALIDATOR.validate(MessageType.MSG_3113,
                VALID_BODY_OMITTED_XML.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid())
                .as("3113 valid body-omitted (minOccurs=0) errors=%s", result.errors())
                .isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void invalid3113_missingSerialNo_shouldFail() {
        ValidationResult result = SHARED_VALIDATOR.validate(MessageType.MSG_3113,
                INVALID_MISSING_SERIAL_NO_XML.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid()).isFalse();
        assertThat(result.errors())
                .as("error must reference missing required body SerialNo field")
                .isNotEmpty()
                .anyMatch(e -> e.contains("SerialNo"));
    }
}
