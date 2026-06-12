package com.puchain.fep.processor.validation;

import com.puchain.fep.converter.type.MessageType;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * XSD-driven validation tests for the 3108 (inbound pzCheckQueryReturn) message body.
 *
 * <p>Coverage (P4-MSG-G T4b，对齐 {@link ProgressQuery3001XsdValidationTest} pattern,
 * 复用 {@link AbstractXsdValidationTest#wrapCfxTemplate}):</p>
 * <ul>
 *     <li>Valid 3108 XML with all required + optional fields (1 pzCheckReturn
 *         row + ExtInfo) passes schema validation</li>
 *     <li>Valid 3108 XML with optional fields omitted (head AddWord + body
 *         pzCheckReturn list + ExtInfo) still passes
 *         (3108.xsd pzCheckReturn minOccurs=0 / ExtInfo minOccurs=0)</li>
 *     <li>Invalid 3108 XML missing required body {@code CheckDate} is rejected</li>
 *     <li>{@link XsdSchemaRegistry} resolves {@link MessageType#MSG_3108}</li>
 * </ul>
 *
 * <p><strong>关键 B2 命名陷阱</strong>: 3108.xsd line 36
 * {@code <xsd:element name="pzCheckQueryReturn3108" type="pzCheckQueryReturn3108">}
 * 实测为 <strong>lowercase p</strong>，与 Java class
 * {@link com.puchain.fep.processor.body.supplychain.PzCheckQueryReturn3108}
 * PascalCase <strong>不一致</strong>（历史 XSD camelCase 例外，3008/3103 是
 * PascalCase 正常）。fixture body element 标签必须用
 * {@code <pzCheckQueryReturn3108>} 否则 XSD 拒绝。</p>
 *
 * <p>DataType.xsd 实测约束（fixture 全部满足）: SerialNo(Text length=30) /
 * NodeCode(String length=14) / OrgCode(String length=14) /
 * TransitionNo(Number length=8) / Result(head Result / 子 pzCheckReturn RetCode
 * 均 Number length=5) / AddWord(Text maxLen=200) / Date(CheckDate YYYYMMDD) /
 * Number1to2(hxqyNum 1-2 位数字) / qyName(hxqyName Text minLen=2, maxLen=50) /
 * qyCode(hxqyCode Text length=18) / Integer(pzCountAll xsd:integer) /
 * Currency(pzAmtAll 金额 pattern) / String0to100(RetMemo maxLen=100)。</p>
 *
 * <p>3108.xsd BatchHead3108 type=ResponseHead (实测 line 31) — 含 Result
 * (length=5) 必填 + AddWord 可选；body element on MSG required (no minOccurs=0,
 * 3108.xsd line 36)。required body 字段：SerialNo / SendNodeCode / DesNodeCode /
 * CheckDate / hxqyNum；子 pzCheckReturn required：hxqyName / hxqyCode / RetCode /
 * pzCountAll / pzAmtAll。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class PzCheckQueryReturn3108XsdValidationTest extends AbstractXsdValidationTest {

    private static final String VALID_FULL_FIELDS_XML = wrapCfxTemplate(
            HNDEMP_NODE, INSTITUTION_NODE, APP_HNDEMP, "3108",
            "31080000000000000001", "31070000000000000001", "20260514", """
                <BatchHead3108>
                  <SendOrgCode>30500000000000</SendOrgCode>
                  <EntrustDate>20260514</EntrustDate>
                  <TransitionNo>00000001</TransitionNo>
                  <Result>10000</Result>
                  <AddWord>对账成功</AddWord>
                </BatchHead3108>
                <pzCheckQueryReturn3108>
                  <SerialNo>SN310800000000000000000000001A</SerialNo>
                  <SendNodeCode>A1000143000104</SendNodeCode>
                  <DesNodeCode>A1000142000001</DesNodeCode>
                  <CheckDate>20260514</CheckDate>
                  <hxqyNum>1</hxqyNum>
                  <pzCheckReturn>
                    <hxqyName>核心企业测试有限公司</hxqyName>
                    <hxqyCode>91110000ABCDEFGH12</hxqyCode>
                    <RetCode>90000</RetCode>
                    <RetMemo>核对一致</RetMemo>
                    <pzCountAll>10</pzCountAll>
                    <pzAmtAll>100000.00</pzAmtAll>
                    <pzFilename>pz_20260514.txt</pzFilename>
                  </pzCheckReturn>
                  <ExtInfo>
                    <ExtData>customExt</ExtData>
                  </ExtInfo>
                </pzCheckQueryReturn3108>""");

    private static final String VALID_OPTIONAL_OMITTED_XML = wrapCfxTemplate(
            HNDEMP_NODE, INSTITUTION_NODE, APP_HNDEMP, "3108",
            "31080000000000000002", "31070000000000000002", "20260514", """
                <BatchHead3108>
                  <SendOrgCode>30500000000000</SendOrgCode>
                  <EntrustDate>20260514</EntrustDate>
                  <TransitionNo>00000002</TransitionNo>
                  <Result>10000</Result>
                </BatchHead3108>
                <pzCheckQueryReturn3108>
                  <SerialNo>SN310800000000000000000000002A</SerialNo>
                  <SendNodeCode>A1000143000104</SendNodeCode>
                  <DesNodeCode>A1000142000001</DesNodeCode>
                  <CheckDate>20260514</CheckDate>
                  <hxqyNum>0</hxqyNum>
                </pzCheckQueryReturn3108>""");

    private static final String INVALID_MISSING_CHECKDATE_XML = wrapCfxTemplate(
            HNDEMP_NODE, INSTITUTION_NODE, APP_HNDEMP, "3108",
            "31080000000000000003", "31070000000000000003", "20260514", """
                <BatchHead3108>
                  <SendOrgCode>30500000000000</SendOrgCode>
                  <EntrustDate>20260514</EntrustDate>
                  <TransitionNo>00000003</TransitionNo>
                  <Result>10000</Result>
                </BatchHead3108>
                <pzCheckQueryReturn3108>
                  <SerialNo>SN310800000000000000000000003A</SerialNo>
                  <SendNodeCode>A1000143000104</SendNodeCode>
                  <DesNodeCode>A1000142000001</DesNodeCode>
                  <hxqyNum>0</hxqyNum>
                </pzCheckQueryReturn3108>""");

    @Test
    void valid3108FullFields_shouldPass() {
        ValidationResult result = validator.validate(MessageType.MSG_3108,
                VALID_FULL_FIELDS_XML.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid())
                .as("3108 valid full fields errors=%s", result.errors())
                .isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void valid3108OptionalOmitted_shouldPass() {
        ValidationResult result = validator.validate(MessageType.MSG_3108,
                VALID_OPTIONAL_OMITTED_XML.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid())
                .as("3108 valid optional-omitted errors=%s", result.errors())
                .isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void invalid3108_missingCheckDate_shouldFail() {
        ValidationResult result = validator.validate(MessageType.MSG_3108,
                INVALID_MISSING_CHECKDATE_XML.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid()).isFalse();
        assertThat(result.errors())
                .as("error must reference missing required body CheckDate field")
                .isNotEmpty()
                .anyMatch(e -> e.contains("CheckDate"));
    }

    @Test
    void registrySupportsMsg3108() {
        assertThat(SHARED_REGISTRY.schemaOf(MessageType.MSG_3108)).isNotNull();
    }
}
