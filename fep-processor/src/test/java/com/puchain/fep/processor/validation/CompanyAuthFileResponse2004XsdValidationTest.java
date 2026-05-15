package com.puchain.fep.processor.validation;

import com.puchain.fep.converter.type.MessageType;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * XSD-driven validation tests for the 2004 (outbound CompanyAuthFileResponse) message body.
 *
 * <p>Coverage (P4-MSG-E T3，对齐 {@link DataTransfer1101XsdValidationTest} pattern):</p>
 * <ul>
 *     <li>Valid 2004 XML with all required + optional fields passes schema validation</li>
 *     <li>Valid 2004 XML with optional IsUpdate/RecordAddWord omitted still passes</li>
 *     <li>Valid 2004 XML with body element entirely omitted still passes (minOccurs="0" on body
 *         — 错误回执场景仅 head 含 Result+AddWord 错误码描述，无 body payload)</li>
 *     <li>Invalid 2004 XML missing required RecordResult is rejected</li>
 *     <li>Invalid 2004 XML violating CompanyCode length=18 / RecordResult length=5 /
 *         RecordAddWord maxLength=200 is rejected
 *         (红线 {@code feedback_xsd_validation_gap} +
 *         {@code feedback_fixture_data_must_satisfy_xsd_constraints})</li>
 *     <li>{@link XsdSchemaRegistry} resolves {@link MessageType#MSG_2004}</li>
 * </ul>
 *
 * <p>DataType.xsd 实测约束: CompanyName(minLen=2, maxLen=100) / CompanyCode(length=18) /
 * AuthBeginDate+AuthEndDate(Date YYYYMMDD) / AuthNo(maxLen=80) / AuthOrgCode(OrgCode length=14) /
 * IsUpdate(Boolean enum 0/1) / RecordResult(Result length=5 number) / RecordAddWord(AddWord
 * maxLen=200). Head 用 ResponseHead 含 Result+AddWord。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class CompanyAuthFileResponse2004XsdValidationTest extends AbstractXsdValidationTest {

    private static final String VALID_FULL_FIELDS_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <CFX>
              <HEAD>
                <Version>1.0</Version>
                <SrcNode>A1000143000104</SrcNode>
                <DesNode>A1000142000001</DesNode>
                <App>FEPx</App>
                <MsgNo>2004</MsgNo>
                <MsgId>20040000000000000001</MsgId>
                <CorrMsgId>10040000000000000001</CorrMsgId>
                <WorkDate>20260511</WorkDate>
              </HEAD>
              <MSG>
                <RealHead2004>
                  <SendOrgCode>30500000000000</SendOrgCode>
                  <EntrustDate>20260511</EntrustDate>
                  <TransitionNo>20260511</TransitionNo>
                  <Result>00000</Result>
                  <AddWord>处理成功</AddWord>
                </RealHead2004>
                <CompanyAuthFileResponse2004>
                  <CompanyName>测试企业有限公司</CompanyName>
                  <CompanyCode>91110000ABCDEFGH12</CompanyCode>
                  <AuthBeginDate>20260101</AuthBeginDate>
                  <AuthEndDate>20271231</AuthEndDate>
                  <AuthNo>AUTH20260511000001</AuthNo>
                  <AuthOrgCode>30500000000099</AuthOrgCode>
                  <IsUpdate>0</IsUpdate>
                  <RecordResult>00000</RecordResult>
                  <RecordAddWord>备案成功</RecordAddWord>
                </CompanyAuthFileResponse2004>
              </MSG>
            </CFX>
            """;

    private static final String VALID_OPTIONAL_OMITTED_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <CFX>
              <HEAD>
                <Version>1.0</Version>
                <SrcNode>A1000143000104</SrcNode>
                <DesNode>A1000142000001</DesNode>
                <App>FEPx</App>
                <MsgNo>2004</MsgNo>
                <MsgId>20040000000000000002</MsgId>
                <CorrMsgId>10040000000000000002</CorrMsgId>
                <WorkDate>20260511</WorkDate>
              </HEAD>
              <MSG>
                <RealHead2004>
                  <SendOrgCode>30500000000000</SendOrgCode>
                  <EntrustDate>20260511</EntrustDate>
                  <TransitionNo>20260511</TransitionNo>
                  <Result>00000</Result>
                </RealHead2004>
                <CompanyAuthFileResponse2004>
                  <CompanyName>测试企业有限公司</CompanyName>
                  <CompanyCode>91110000ABCDEFGH12</CompanyCode>
                  <AuthBeginDate>20260101</AuthBeginDate>
                  <AuthEndDate>20271231</AuthEndDate>
                  <AuthNo>AUTH20260511000001</AuthNo>
                  <AuthOrgCode>30500000000099</AuthOrgCode>
                  <RecordResult>00000</RecordResult>
                </CompanyAuthFileResponse2004>
              </MSG>
            </CFX>
            """;

    /**
     * 错误回执场景：head 仅 Result+AddWord 错误码描述，body element 整段缺失。
     */
    private static final String VALID_EMPTY_BODY_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <CFX>
              <HEAD>
                <Version>1.0</Version>
                <SrcNode>A1000143000104</SrcNode>
                <DesNode>A1000142000001</DesNode>
                <App>FEPx</App>
                <MsgNo>2004</MsgNo>
                <MsgId>20040000000000000003</MsgId>
                <CorrMsgId>10040000000000000003</CorrMsgId>
                <WorkDate>20260511</WorkDate>
              </HEAD>
              <MSG>
                <RealHead2004>
                  <SendOrgCode>30500000000000</SendOrgCode>
                  <EntrustDate>20260511</EntrustDate>
                  <TransitionNo>20260511</TransitionNo>
                  <Result>99999</Result>
                  <AddWord>授权书已存在，请勿重复提交</AddWord>
                </RealHead2004>
              </MSG>
            </CFX>
            """;

    private static final String INVALID_MISSING_RECORD_RESULT_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <CFX>
              <HEAD>
                <Version>1.0</Version>
                <SrcNode>A1000143000104</SrcNode>
                <DesNode>A1000142000001</DesNode>
                <App>FEPx</App>
                <MsgNo>2004</MsgNo>
                <MsgId>20040000000000000004</MsgId>
                <CorrMsgId>10040000000000000004</CorrMsgId>
                <WorkDate>20260511</WorkDate>
              </HEAD>
              <MSG>
                <RealHead2004>
                  <SendOrgCode>30500000000000</SendOrgCode>
                  <EntrustDate>20260511</EntrustDate>
                  <TransitionNo>20260511</TransitionNo>
                  <Result>00000</Result>
                </RealHead2004>
                <CompanyAuthFileResponse2004>
                  <CompanyName>测试企业有限公司</CompanyName>
                  <CompanyCode>91110000ABCDEFGH12</CompanyCode>
                  <AuthBeginDate>20260101</AuthBeginDate>
                  <AuthEndDate>20271231</AuthEndDate>
                  <AuthNo>AUTH20260511000001</AuthNo>
                  <AuthOrgCode>30500000000099</AuthOrgCode>
                </CompanyAuthFileResponse2004>
              </MSG>
            </CFX>
            """;

    @Test
    void valid2004FullFields_shouldPass() {
        ValidationResult result = validator.validate(MessageType.MSG_2004,
                VALID_FULL_FIELDS_XML.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void valid2004OptionalOmitted_shouldPass() {
        ValidationResult result = validator.validate(MessageType.MSG_2004,
                VALID_OPTIONAL_OMITTED_XML.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void valid2004_emptyBody_shouldPass() {
        ValidationResult result = validator.validate(MessageType.MSG_2004,
                VALID_EMPTY_BODY_XML.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void invalid2004_missingRecordResult_shouldFail() {
        ValidationResult result = validator.validate(MessageType.MSG_2004,
                INVALID_MISSING_RECORD_RESULT_XML.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid()).isFalse();
        assertThat(result.errors())
                .as("error must reference missing RecordResult field")
                .isNotEmpty()
                .anyMatch(e -> e.contains("RecordResult"));
    }

    @Test
    void invalid2004_recordAddWordOverMaxLength_shouldFail() {
        // DataType.xsd AddWord maxLength=200 实测，overflow = 201
        String overflowWord = "X".repeat(201);
        String xml = VALID_FULL_FIELDS_XML.replace(
                "<RecordAddWord>备案成功</RecordAddWord>",
                "<RecordAddWord>" + overflowWord + "</RecordAddWord>");

        ValidationResult result = validator.validate(MessageType.MSG_2004,
                xml.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid()).isFalse();
        assertThat(result.errors())
                .as("error must reference RecordAddWord maxLength=200 violation")
                .isNotEmpty()
                .anyMatch(e -> e.contains("RecordAddWord"));
    }

    @Test
    void invalid2004_companyCodeWrongLength_shouldFail() {
        // DataType.xsd CompanyCode length=18 实测，violation 用 17 位
        String xml = VALID_FULL_FIELDS_XML.replace(
                "<CompanyCode>91110000ABCDEFGH12</CompanyCode>",
                "<CompanyCode>91110000ABCDEFGH1</CompanyCode>");

        ValidationResult result = validator.validate(MessageType.MSG_2004,
                xml.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid()).isFalse();
        assertThat(result.errors())
                .as("error must reference CompanyCode length=18 violation")
                .isNotEmpty()
                .anyMatch(e -> e.contains("CompanyCode"));
    }

    @Test
    void registry_should_supports_msg_2004() {
        assertThat(SHARED_REGISTRY.schemaOf(MessageType.MSG_2004)).isNotNull();
    }
}
