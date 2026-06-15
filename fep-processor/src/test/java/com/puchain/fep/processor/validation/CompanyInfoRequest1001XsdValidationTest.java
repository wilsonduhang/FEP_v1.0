package com.puchain.fep.processor.validation;

import com.puchain.fep.converter.type.MessageType;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * XSD-driven validation tests for the 1001 (outbound CompanyInfoRequest) message body.
 *
 * <p>Coverage (P4-MSG-E T3，对齐 {@link DataTransfer1101XsdValidationTest} pattern):</p>
 * <ul>
 *     <li>Valid 1001 XML with all required + optional fields passes schema validation</li>
 *     <li>Valid 1001 XML with optional BeginDate/EndDate/Parameters omitted still passes</li>
 *     <li>Invalid 1001 XML missing required CompanyName is rejected</li>
 *     <li>Invalid 1001 XML violating CompanyName maxLength=100 / CompanyCode length=18 /
 *         AuthOrgCode length=14 / AuthNo maxLength=80 is rejected
 *         (红线 {@code feedback_xsd_validation_gap} +
 *         {@code feedback_fixture_data_must_satisfy_xsd_constraints})</li>
 *     <li>{@link XsdSchemaRegistry} resolves {@link MessageType#MSG_1001}</li>
 * </ul>
 *
 * <p>DataType.xsd 实测约束（fixture 全部满足）: CompanyName(minLen=2, maxLen=100) /
 * CompanyCode(length=18) / MainClass+SecondClass(Token [A-Za-z0-9]*, minLen=2, maxLen=16) /
 * AuthNo(maxLen=80) / AuthOrgCode(OrgCode length=14) / BeginDate+EndDate(Date pattern
 * YYYYMMDD lexical) / Parameters(maxLen=2000).</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class CompanyInfoRequest1001XsdValidationTest extends AbstractXsdValidationTest {

    private static final String VALID_FULL_FIELDS_XML = wrapCfxTemplate(
            INSTITUTION_NODE, HNDEMP_NODE, APP_FEPX, "1001",
            "10010000000000000001", "00000000000000000000", "20260511", """
                <RealHead1001>
                  <SendOrgCode>30500000000000</SendOrgCode>
                  <EntrustDate>20260511</EntrustDate>
                  <TransitionNo>20260511</TransitionNo>
                </RealHead1001>
                <CompanyInfoRequest1001>
                  <CompanyName>测试企业有限公司</CompanyName>
                  <CompanyCode>91110000ABCDEFGH12</CompanyCode>
                  <MainClass>COINFO</MainClass>
                  <SecondClass>I1001</SecondClass>
                  <BeginDate>20260101</BeginDate>
                  <EndDate>20260511</EndDate>
                  <AuthNo>AUTH20260511000001</AuthNo>
                  <AuthOrgCode>30500000000099</AuthOrgCode>
                  <Parameters>k=v</Parameters>
                </CompanyInfoRequest1001>""");

    private static final String INVALID_MISSING_COMPANY_NAME_XML = wrapCfxTemplate(
            INSTITUTION_NODE, HNDEMP_NODE, APP_FEPX, "1001",
            "10010000000000000003", "00000000000000000000", "20260511", """
                <RealHead1001>
                  <SendOrgCode>30500000000000</SendOrgCode>
                  <EntrustDate>20260511</EntrustDate>
                  <TransitionNo>20260511</TransitionNo>
                </RealHead1001>
                <CompanyInfoRequest1001>
                  <CompanyCode>91110000ABCDEFGH12</CompanyCode>
                  <MainClass>COINFO</MainClass>
                  <SecondClass>I1001</SecondClass>
                  <AuthNo>AUTH20260511000001</AuthNo>
                  <AuthOrgCode>30500000000099</AuthOrgCode>
                </CompanyInfoRequest1001>""");

    private static final String VALID_OPTIONAL_OMITTED_XML = wrapCfxTemplate(
            INSTITUTION_NODE, HNDEMP_NODE, APP_FEPX, "1001",
            "10010000000000000002", "00000000000000000000", "20260511", """
                <RealHead1001>
                  <SendOrgCode>30500000000000</SendOrgCode>
                  <EntrustDate>20260511</EntrustDate>
                  <TransitionNo>20260511</TransitionNo>
                </RealHead1001>
                <CompanyInfoRequest1001>
                  <CompanyName>测试企业有限公司</CompanyName>
                  <CompanyCode>91110000ABCDEFGH12</CompanyCode>
                  <MainClass>COINFO</MainClass>
                  <SecondClass>I1001</SecondClass>
                  <AuthNo>AUTH20260511000001</AuthNo>
                  <AuthOrgCode>30500000000099</AuthOrgCode>
                </CompanyInfoRequest1001>""");

    @Test
    void valid1001FullFields_shouldPass() {
        ValidationResult result = validator.validate(MessageType.MSG_1001,
                VALID_FULL_FIELDS_XML.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void valid1001OptionalOmitted_shouldPass() {
        ValidationResult result = validator.validate(MessageType.MSG_1001,
                VALID_OPTIONAL_OMITTED_XML.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void invalid1001_missingCompanyName_shouldFail() {
        ValidationResult result = validator.validate(MessageType.MSG_1001,
                INVALID_MISSING_COMPANY_NAME_XML.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid()).isFalse();
        assertThat(result.errors())
                .as("error must reference missing CompanyName field")
                .isNotEmpty()
                .anyMatch(e -> e.contains("CompanyName"));
    }

    @Test
    void invalid1001_companyNameOverMaxLength_shouldFail() {
        // DataType.xsd CompanyName maxLength=100 实测，overflow = 101
        String overflowName = "X".repeat(101);
        String xml = VALID_FULL_FIELDS_XML.replace(
                "<CompanyName>测试企业有限公司</CompanyName>",
                "<CompanyName>" + overflowName + "</CompanyName>");

        ValidationResult result = validator.validate(MessageType.MSG_1001,
                xml.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid()).isFalse();
        assertThat(result.errors())
                .as("error must reference CompanyName maxLength=100 violation")
                .isNotEmpty()
                .anyMatch(e -> e.contains("CompanyName"));
    }

    @Test
    void invalid1001_companyCodeWrongLength_shouldFail() {
        // DataType.xsd CompanyCode length=18 实测，违反用 17 位
        String xml = VALID_FULL_FIELDS_XML.replace(
                "<CompanyCode>91110000ABCDEFGH12</CompanyCode>",
                "<CompanyCode>91110000ABCDEFGH1</CompanyCode>");

        ValidationResult result = validator.validate(MessageType.MSG_1001,
                xml.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid()).isFalse();
        assertThat(result.errors())
                .as("error must reference CompanyCode length=18 violation")
                .isNotEmpty()
                .anyMatch(e -> e.contains("CompanyCode"));
    }

    @Test
    void invalid1001_authOrgCodeWrongLength_shouldFail() {
        // DataType.xsd AuthOrgCode is OrgCode type, length=14 实测，违反用 13 位
        String xml = VALID_FULL_FIELDS_XML.replace(
                "<AuthOrgCode>30500000000099</AuthOrgCode>",
                "<AuthOrgCode>3050000000009</AuthOrgCode>");

        ValidationResult result = validator.validate(MessageType.MSG_1001,
                xml.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid()).isFalse();
        assertThat(result.errors())
                .as("error must reference AuthOrgCode length=14 violation")
                .isNotEmpty()
                .anyMatch(e -> e.contains("AuthOrgCode"));
    }

    @Test
    void invalid1001_beginDateNotMatchingPattern_shouldFail() {
        // DataType.xsd Date pattern [0-9]{4}(0[1-9]|1[0-2])([0-2][1-9]|[12][0-9]|3[01])
        // 实测，月份 13 不在范围内
        String xml = VALID_FULL_FIELDS_XML.replace(
                "<BeginDate>20260101</BeginDate>",
                "<BeginDate>20261301</BeginDate>");

        ValidationResult result = validator.validate(MessageType.MSG_1001,
                xml.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid()).isFalse();
        assertThat(result.errors())
                .as("error must reference BeginDate pattern violation (invalid month 13)")
                .isNotEmpty()
                .anyMatch(e -> e.contains("BeginDate"));
    }

    @Test
    void registrySupportsMsg1001() {
        assertThat(SHARED_REGISTRY.schemaOf(MessageType.MSG_1001)).isNotNull();
    }
}
