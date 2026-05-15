package com.puchain.fep.processor.validation;

import com.puchain.fep.converter.type.MessageType;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * XSD-driven validation tests for the 2001 (outbound CompanyInfoResponse) message body.
 *
 * <p>Coverage (P4-MSG-E T3，对齐 {@link DataTransfer1101XsdValidationTest} pattern):</p>
 * <ul>
 *     <li>Valid 2001 XML with all required + optional fields passes schema validation</li>
 *     <li>Valid 2001 XML with optional BeginDate/EndDate/QueryAddWord omitted still passes</li>
 *     <li>Valid 2001 XML with body element entirely omitted still passes (minOccurs="0" on body
 *         — 错误回执场景仅 head 含 Result+AddWord 错误码描述，无 body payload)</li>
 *     <li>Invalid 2001 XML missing required QueryResult is rejected</li>
 *     <li>Invalid 2001 XML violating CompanyName maxLength=100 / QueryResult length=5 is rejected
 *         (红线 {@code feedback_xsd_validation_gap} +
 *         {@code feedback_fixture_data_must_satisfy_xsd_constraints})</li>
 *     <li>{@link XsdSchemaRegistry} resolves {@link MessageType#MSG_2001}</li>
 * </ul>
 *
 * <p>DataType.xsd 实测约束: CompanyName(minLen=2, maxLen=100) / CompanyCode(length=18) /
 * MainClass+SecondClass(Token, minLen=2, maxLen=16) / Date(YYYYMMDD) / QueryResult(Result
 * length=5 number) / QueryAddWord(AddWord maxLen=200). Head 用 ResponseHead 含 Result+AddWord。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class CompanyInfoResponse2001XsdValidationTest extends AbstractXsdValidationTest {

    private static final String VALID_FULL_FIELDS_XML = wrapCfxTemplate(
            "A1000143000104", "A1000142000001", "FEPx", "2001",
            "20010000000000000001", "10010000000000000001", "20260511", """
                <RealHead2001>
                  <SendOrgCode>30500000000000</SendOrgCode>
                  <EntrustDate>20260511</EntrustDate>
                  <TransitionNo>20260511</TransitionNo>
                  <Result>00000</Result>
                  <AddWord>处理成功</AddWord>
                </RealHead2001>
                <CompanyInfoResponse2001>
                  <CompanyName>测试企业有限公司</CompanyName>
                  <CompanyCode>91110000ABCDEFGH12</CompanyCode>
                  <MainClass>QYXX</MainClass>
                  <SecondClass>QYXX01</SecondClass>
                  <BeginDate>20260101</BeginDate>
                  <EndDate>20260511</EndDate>
                  <QueryResult>00000</QueryResult>
                  <QueryAddWord>查询成功</QueryAddWord>
                </CompanyInfoResponse2001>""");

    private static final String VALID_OPTIONAL_OMITTED_XML = wrapCfxTemplate(
            "A1000143000104", "A1000142000001", "FEPx", "2001",
            "20010000000000000002", "10010000000000000002", "20260511", """
                <RealHead2001>
                  <SendOrgCode>30500000000000</SendOrgCode>
                  <EntrustDate>20260511</EntrustDate>
                  <TransitionNo>20260511</TransitionNo>
                  <Result>00000</Result>
                </RealHead2001>
                <CompanyInfoResponse2001>
                  <CompanyName>测试企业有限公司</CompanyName>
                  <CompanyCode>91110000ABCDEFGH12</CompanyCode>
                  <MainClass>QYXX</MainClass>
                  <SecondClass>QYXX01</SecondClass>
                  <QueryResult>00000</QueryResult>
                </CompanyInfoResponse2001>""");

    /**
     * 错误回执场景：head 仅 Result+AddWord 错误码描述，body element 整段缺失
     * (XSD 2001.xsd:36 CompanyInfoResponse2001 minOccurs="0" 允许)。
     */
    private static final String VALID_EMPTY_BODY_XML = wrapCfxTemplate(
            "A1000143000104", "A1000142000001", "FEPx", "2001",
            "20010000000000000003", "10010000000000000003", "20260511", """
                <RealHead2001>
                  <SendOrgCode>30500000000000</SendOrgCode>
                  <EntrustDate>20260511</EntrustDate>
                  <TransitionNo>20260511</TransitionNo>
                  <Result>99999</Result>
                  <AddWord>查询授权过期，请重新申请授权</AddWord>
                </RealHead2001>""");

    private static final String INVALID_MISSING_QUERY_RESULT_XML = wrapCfxTemplate(
            "A1000143000104", "A1000142000001", "FEPx", "2001",
            "20010000000000000004", "10010000000000000004", "20260511", """
                <RealHead2001>
                  <SendOrgCode>30500000000000</SendOrgCode>
                  <EntrustDate>20260511</EntrustDate>
                  <TransitionNo>20260511</TransitionNo>
                  <Result>00000</Result>
                </RealHead2001>
                <CompanyInfoResponse2001>
                  <CompanyName>测试企业有限公司</CompanyName>
                  <CompanyCode>91110000ABCDEFGH12</CompanyCode>
                  <MainClass>QYXX</MainClass>
                  <SecondClass>QYXX01</SecondClass>
                </CompanyInfoResponse2001>""");

    @Test
    void valid2001FullFields_shouldPass() {
        ValidationResult result = validator.validate(MessageType.MSG_2001,
                VALID_FULL_FIELDS_XML.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void valid2001OptionalOmitted_shouldPass() {
        ValidationResult result = validator.validate(MessageType.MSG_2001,
                VALID_OPTIONAL_OMITTED_XML.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void valid2001_emptyBody_shouldPass() {
        // 2001.xsd body minOccurs="0" — 错误回执场景仅 head 含 Result+AddWord 错误码描述
        ValidationResult result = validator.validate(MessageType.MSG_2001,
                VALID_EMPTY_BODY_XML.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void invalid2001_missingQueryResult_shouldFail() {
        ValidationResult result = validator.validate(MessageType.MSG_2001,
                INVALID_MISSING_QUERY_RESULT_XML.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid()).isFalse();
        assertThat(result.errors())
                .as("error must reference missing QueryResult field")
                .isNotEmpty()
                .anyMatch(e -> e.contains("QueryResult"));
    }

    @Test
    void invalid2001_companyNameOverMaxLength_shouldFail() {
        String overflowName = "X".repeat(101);
        String xml = VALID_FULL_FIELDS_XML.replace(
                "<CompanyName>测试企业有限公司</CompanyName>",
                "<CompanyName>" + overflowName + "</CompanyName>");

        ValidationResult result = validator.validate(MessageType.MSG_2001,
                xml.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid()).isFalse();
        assertThat(result.errors())
                .as("error must reference CompanyName maxLength=100 violation")
                .isNotEmpty()
                .anyMatch(e -> e.contains("CompanyName"));
    }

    @Test
    void invalid2001_queryResultWrongLength_shouldFail() {
        // DataType.xsd Result type length=5 实测，violation 用 4 位
        String xml = VALID_FULL_FIELDS_XML.replace(
                "<QueryResult>00000</QueryResult>",
                "<QueryResult>0000</QueryResult>");

        ValidationResult result = validator.validate(MessageType.MSG_2001,
                xml.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid()).isFalse();
        assertThat(result.errors())
                .as("error must reference QueryResult length=5 violation")
                .isNotEmpty()
                .anyMatch(e -> e.contains("QueryResult"));
    }

    @Test
    void registrySupportsMsg2001() {
        assertThat(SHARED_REGISTRY.schemaOf(MessageType.MSG_2001)).isNotNull();
    }
}
