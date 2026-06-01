package com.puchain.fep.processor.validation;

import com.puchain.fep.converter.type.MessageType;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * XSD-driven validation tests for the 1004 (outbound CompanyAuthFileTransfer) message body.
 *
 * <p>Coverage (P4-MSG-E T3，对齐 {@link DataTransfer1101XsdValidationTest} pattern):</p>
 * <ul>
 *     <li>Valid 1004 XML with all required + optional fields passes schema validation</li>
 *     <li>Valid 1004 XML with optional IsUpdate/Parameters omitted still passes</li>
 *     <li>Invalid 1004 XML missing required AuthBeginDate is rejected</li>
 *     <li>Invalid 1004 XML violating CompanyCode length=18 / AuthNo maxLength=80 /
 *         IsUpdate enum (only "0"/"1") is rejected
 *         (红线 {@code feedback_xsd_validation_gap} +
 *         {@code feedback_fixture_data_must_satisfy_xsd_constraints})</li>
 *     <li>{@link XsdSchemaRegistry} resolves {@link MessageType#MSG_1004}</li>
 * </ul>
 *
 * <p>DataType.xsd 实测约束: CompanyName(minLen=2, maxLen=100) / CompanyCode(length=18) /
 * AuthBeginDate+AuthEndDate(Date YYYYMMDD) / AuthNo(maxLen=80) / AuthOrgCode(OrgCode length=14) /
 * IsUpdate(Boolean enum 0/1) / Parameters(maxLen=2000)。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class CompanyAuthFileTransfer1004XsdValidationTest extends AbstractXsdValidationTest {

    private static final String VALID_FULL_FIELDS_XML = wrapCfxTemplate(
            INSTITUTION_NODE, HNDEMP_NODE, APP_FEPX, "1004",
            "10040000000000000001", "00000000000000000000", "20260511", """
                <RealHead1004>
                  <SendOrgCode>30500000000000</SendOrgCode>
                  <EntrustDate>20260511</EntrustDate>
                  <TransitionNo>20260511</TransitionNo>
                </RealHead1004>
                <CompanyAuthFileTransfer1004>
                  <CompanyName>测试企业有限公司</CompanyName>
                  <CompanyCode>91110000ABCDEFGH12</CompanyCode>
                  <AuthBeginDate>20260101</AuthBeginDate>
                  <AuthEndDate>20271231</AuthEndDate>
                  <AuthNo>AUTH20260511000001</AuthNo>
                  <AuthOrgCode>30500000000099</AuthOrgCode>
                  <IsUpdate>0</IsUpdate>
                  <Parameters>k=v</Parameters>
                </CompanyAuthFileTransfer1004>""");

    private static final String VALID_OPTIONAL_OMITTED_XML = wrapCfxTemplate(
            INSTITUTION_NODE, HNDEMP_NODE, APP_FEPX, "1004",
            "10040000000000000002", "00000000000000000000", "20260511", """
                <RealHead1004>
                  <SendOrgCode>30500000000000</SendOrgCode>
                  <EntrustDate>20260511</EntrustDate>
                  <TransitionNo>20260511</TransitionNo>
                </RealHead1004>
                <CompanyAuthFileTransfer1004>
                  <CompanyName>测试企业有限公司</CompanyName>
                  <CompanyCode>91110000ABCDEFGH12</CompanyCode>
                  <AuthBeginDate>20260101</AuthBeginDate>
                  <AuthEndDate>20271231</AuthEndDate>
                  <AuthNo>AUTH20260511000001</AuthNo>
                  <AuthOrgCode>30500000000099</AuthOrgCode>
                </CompanyAuthFileTransfer1004>""");

    private static final String INVALID_MISSING_AUTH_BEGIN_DATE_XML = wrapCfxTemplate(
            INSTITUTION_NODE, HNDEMP_NODE, APP_FEPX, "1004",
            "10040000000000000003", "00000000000000000000", "20260511", """
                <RealHead1004>
                  <SendOrgCode>30500000000000</SendOrgCode>
                  <EntrustDate>20260511</EntrustDate>
                  <TransitionNo>20260511</TransitionNo>
                </RealHead1004>
                <CompanyAuthFileTransfer1004>
                  <CompanyName>测试企业有限公司</CompanyName>
                  <CompanyCode>91110000ABCDEFGH12</CompanyCode>
                  <AuthEndDate>20271231</AuthEndDate>
                  <AuthNo>AUTH20260511000001</AuthNo>
                  <AuthOrgCode>30500000000099</AuthOrgCode>
                </CompanyAuthFileTransfer1004>""");

    @Test
    void valid1004FullFields_shouldPass() {
        ValidationResult result = validator.validate(MessageType.MSG_1004,
                VALID_FULL_FIELDS_XML.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void valid1004OptionalOmitted_shouldPass() {
        ValidationResult result = validator.validate(MessageType.MSG_1004,
                VALID_OPTIONAL_OMITTED_XML.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void invalid1004_missingAuthBeginDate_shouldFail() {
        ValidationResult result = validator.validate(MessageType.MSG_1004,
                INVALID_MISSING_AUTH_BEGIN_DATE_XML.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid()).isFalse();
        assertThat(result.errors())
                .as("error must reference missing AuthBeginDate field")
                .isNotEmpty()
                .anyMatch(e -> e.contains("AuthBeginDate"));
    }

    @Test
    void invalid1004_authNoOverMaxLength_shouldFail() {
        // DataType.xsd AuthNo maxLength=80 实测，overflow = 81
        String overflowAuth = "A".repeat(81);
        String xml = VALID_FULL_FIELDS_XML.replace(
                "<AuthNo>AUTH20260511000001</AuthNo>",
                "<AuthNo>" + overflowAuth + "</AuthNo>");

        ValidationResult result = validator.validate(MessageType.MSG_1004,
                xml.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid()).isFalse();
        assertThat(result.errors())
                .as("error must reference AuthNo maxLength=80 violation")
                .isNotEmpty()
                .anyMatch(e -> e.contains("AuthNo"));
    }

    @Test
    void invalid1004_isUpdateNotInEnum_shouldFail() {
        // DataType.xsd Boolean enum {0, 1}，violation 用 "2"
        String xml = VALID_FULL_FIELDS_XML.replace(
                "<IsUpdate>0</IsUpdate>",
                "<IsUpdate>2</IsUpdate>");

        ValidationResult result = validator.validate(MessageType.MSG_1004,
                xml.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid()).isFalse();
        assertThat(result.errors())
                .as("error must reference IsUpdate enum violation (only 0/1 allowed)")
                .isNotEmpty()
                .anyMatch(e -> e.contains("IsUpdate"));
    }

    @Test
    void invalid1004_authEndDateNotMatchingPattern_shouldFail() {
        // DataType.xsd Date pattern lexical 月份必须 01-12，violation 用 13
        String xml = VALID_FULL_FIELDS_XML.replace(
                "<AuthEndDate>20271231</AuthEndDate>",
                "<AuthEndDate>20271301</AuthEndDate>");

        ValidationResult result = validator.validate(MessageType.MSG_1004,
                xml.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid()).isFalse();
        assertThat(result.errors())
                .as("error must reference AuthEndDate pattern violation (invalid month 13)")
                .isNotEmpty()
                .anyMatch(e -> e.contains("AuthEndDate"));
    }

    @Test
    void registrySupportsMsg1004() {
        assertThat(SHARED_REGISTRY.schemaOf(MessageType.MSG_1004)).isNotNull();
    }
}
