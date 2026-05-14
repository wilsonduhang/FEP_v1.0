package com.puchain.fep.processor.validation;

import com.puchain.fep.converter.type.MessageType;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * XSD-driven validation tests for the 3006 (outbound qyAccQueryReturn) message body.
 *
 * <p>Coverage (P4-MSG-F T3，对齐 {@link CompanyInfoRequest1001XsdValidationTest} pattern):</p>
 * <ul>
 *     <li>Valid 3006 XML with all required + optional fields (AccReturnMemo + ExtInfo) passes schema validation</li>
 *     <li>Valid 3006 XML with optional fields omitted (AccReturnMemo + ExtInfo + AddWord) still passes</li>
 *     <li>Invalid 3006 XML missing required {@code AccReturnCode} is rejected</li>
 *     <li>{@link XsdSchemaRegistry} resolves {@link MessageType#MSG_3006}</li>
 * </ul>
 *
 * <p>DataType.xsd 实测约束: SerialNo(length=30) / NodeCode(length=14) / OrgCode(length=14) /
 * TransitionNo(Number length=8) / AccName(Text minLen=2, maxLen=50) /
 * AccNumber(Number minLen=10, maxLen=30) / Number1to2(AccReturnCode 1-2 位数字) /
 * String0to200(AccReturnMemo maxLen=200) / Result(Number length=5) / AddWord(Text maxLen=200).</p>
 *
 * <p>3006.xsd RealHead3006 type=ResponseHead (实测 line 31) — 含 Result (length=5) 必填 +
 * AddWord 可选；body root {@code qyAccQueryReturn3006} <strong>camelCase</strong> + body
 * element on MSG is {@code minOccurs="0"} (3006.xsd line 36)。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class QyAccQueryReturn3006XsdValidationTest extends AbstractXsdValidationTest {

    private static final String VALID_FULL_FIELDS_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <CFX>
              <HEAD>
                <Version>1.0</Version>
                <SrcNode>A1000143000104</SrcNode>
                <DesNode>A1000142000001</DesNode>
                <App>FEPx</App>
                <MsgNo>3006</MsgNo>
                <MsgId>30060000000000000001</MsgId>
                <CorrMsgId>30050000000000000001</CorrMsgId>
                <WorkDate>20260513</WorkDate>
              </HEAD>
              <MSG>
                <RealHead3006>
                  <SendOrgCode>30500000000000</SendOrgCode>
                  <EntrustDate>20260513</EntrustDate>
                  <TransitionNo>00000001</TransitionNo>
                  <Result>10000</Result>
                  <AddWord>查询成功</AddWord>
                </RealHead3006>
                <qyAccQueryReturn3006>
                  <SerialNo>SN300600000000000000000000001A</SerialNo>
                  <SendNodeCode>A1000143000104</SendNodeCode>
                  <DesNodeCode>A1000142000001</DesNodeCode>
                  <qyAccName>测试企业账户</qyAccName>
                  <qyAccCode>6228480000000000</qyAccCode>
                  <AccReturnCode>1</AccReturnCode>
                  <AccReturnMemo>账户正常</AccReturnMemo>
                  <ExtInfo>
                    <ExtData>customExt</ExtData>
                  </ExtInfo>
                </qyAccQueryReturn3006>
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
                <MsgNo>3006</MsgNo>
                <MsgId>30060000000000000002</MsgId>
                <CorrMsgId>30050000000000000002</CorrMsgId>
                <WorkDate>20260513</WorkDate>
              </HEAD>
              <MSG>
                <RealHead3006>
                  <SendOrgCode>30500000000000</SendOrgCode>
                  <EntrustDate>20260513</EntrustDate>
                  <TransitionNo>00000002</TransitionNo>
                  <Result>10000</Result>
                </RealHead3006>
                <qyAccQueryReturn3006>
                  <SerialNo>SN300600000000000000000000002A</SerialNo>
                  <SendNodeCode>A1000143000104</SendNodeCode>
                  <DesNodeCode>A1000142000001</DesNodeCode>
                  <qyAccName>测试企业账户</qyAccName>
                  <qyAccCode>6228480000000000</qyAccCode>
                  <AccReturnCode>1</AccReturnCode>
                </qyAccQueryReturn3006>
              </MSG>
            </CFX>
            """;

    private static final String INVALID_MISSING_ACCRETURNCODE_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <CFX>
              <HEAD>
                <Version>1.0</Version>
                <SrcNode>A1000143000104</SrcNode>
                <DesNode>A1000142000001</DesNode>
                <App>FEPx</App>
                <MsgNo>3006</MsgNo>
                <MsgId>30060000000000000003</MsgId>
                <CorrMsgId>30050000000000000003</CorrMsgId>
                <WorkDate>20260513</WorkDate>
              </HEAD>
              <MSG>
                <RealHead3006>
                  <SendOrgCode>30500000000000</SendOrgCode>
                  <EntrustDate>20260513</EntrustDate>
                  <TransitionNo>00000003</TransitionNo>
                  <Result>10000</Result>
                </RealHead3006>
                <qyAccQueryReturn3006>
                  <SerialNo>SN300600000000000000000000003A</SerialNo>
                  <SendNodeCode>A1000143000104</SendNodeCode>
                  <DesNodeCode>A1000142000001</DesNodeCode>
                  <qyAccName>测试企业账户</qyAccName>
                  <qyAccCode>6228480000000000</qyAccCode>
                </qyAccQueryReturn3006>
              </MSG>
            </CFX>
            """;

    @Test
    void valid3006FullFields_shouldPass() {
        ValidationResult result = validator.validate(MessageType.MSG_3006,
                VALID_FULL_FIELDS_XML.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void valid3006OptionalOmitted_shouldPass() {
        ValidationResult result = validator.validate(MessageType.MSG_3006,
                VALID_OPTIONAL_OMITTED_XML.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void invalid3006_missingAccReturnCode_shouldFail() {
        ValidationResult result = validator.validate(MessageType.MSG_3006,
                INVALID_MISSING_ACCRETURNCODE_XML.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid()).isFalse();
        assertThat(result.errors())
                .as("error must reference missing AccReturnCode field")
                .isNotEmpty()
                .anyMatch(e -> e.contains("AccReturnCode"));
    }
}
