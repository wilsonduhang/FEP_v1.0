package com.puchain.fep.processor.validation;

import com.puchain.fep.converter.type.MessageType;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * XSD-driven validation tests for the 3002 (outbound ProgressQueryReturn) message body.
 *
 * <p>Coverage (P4-MSG-F T3，对齐 {@link CompanyInfoRequest1001XsdValidationTest} pattern):</p>
 * <ul>
 *     <li>Valid 3002 XML with all required + optional fields (ReturnMemo + ExtInfo) passes schema validation</li>
 *     <li>Valid 3002 XML with optional fields omitted (ReturnMemo + ExtInfo + AddWord) still passes</li>
 *     <li>Invalid 3002 XML missing required {@code ReturnCode} is rejected</li>
 *     <li>{@link XsdSchemaRegistry} resolves {@link MessageType#MSG_3002}</li>
 * </ul>
 *
 * <p>DataType.xsd 实测约束: SerialNo(length=30) / NodeCode(length=14) / OrgCode(length=14) /
 * TransitionNo(Number length=8) / qyName(minLen=2, maxLen=50) / qyCode(length=18) /
 * Number1to2(QueryType 1-2 位) / String0to100(QueryKey maxLen=100) / Number2(ReturnCode length=2) /
 * Result(Number length=5) / AddWord(Text maxLen=200).</p>
 *
 * <p>3002.xsd RealHead3002 type=ResponseHead (实测 line 31) — 含 Result (length=5) 必填 +
 * AddWord 可选；body root {@code ProgressQueryReturn3002} PascalCase + body element on MSG
 * is {@code minOccurs="0"} (3002.xsd line 36).</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class ProgressQueryReturn3002XsdValidationTest extends AbstractXsdValidationTest {

    private static final String VALID_FULL_FIELDS_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <CFX>
              <HEAD>
                <Version>1.0</Version>
                <SrcNode>A1000143000104</SrcNode>
                <DesNode>A1000142000001</DesNode>
                <App>FEPx</App>
                <MsgNo>3002</MsgNo>
                <MsgId>30020000000000000001</MsgId>
                <CorrMsgId>30010000000000000001</CorrMsgId>
                <WorkDate>20260513</WorkDate>
              </HEAD>
              <MSG>
                <RealHead3002>
                  <SendOrgCode>30500000000000</SendOrgCode>
                  <EntrustDate>20260513</EntrustDate>
                  <TransitionNo>00000001</TransitionNo>
                  <Result>10000</Result>
                  <AddWord>查询成功</AddWord>
                </RealHead3002>
                <ProgressQueryReturn3002>
                  <SerialNo>SN300200000000000000000000001A</SerialNo>
                  <SendNodeCode>A1000143000104</SendNodeCode>
                  <DesNodeCode>A1000142000001</DesNodeCode>
                  <hxqyName>核心企业测试有限公司</hxqyName>
                  <hxqyCode>91110000ABCDEFGH12</hxqyCode>
                  <QueryType>1</QueryType>
                  <QueryKey>BIZ20260513000001</QueryKey>
                  <ReturnCode>01</ReturnCode>
                  <ReturnMemo>查询成功</ReturnMemo>
                  <ExtInfo>
                    <ExtData>customExt</ExtData>
                  </ExtInfo>
                </ProgressQueryReturn3002>
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
                <MsgNo>3002</MsgNo>
                <MsgId>30020000000000000002</MsgId>
                <CorrMsgId>30010000000000000002</CorrMsgId>
                <WorkDate>20260513</WorkDate>
              </HEAD>
              <MSG>
                <RealHead3002>
                  <SendOrgCode>30500000000000</SendOrgCode>
                  <EntrustDate>20260513</EntrustDate>
                  <TransitionNo>00000002</TransitionNo>
                  <Result>10000</Result>
                </RealHead3002>
                <ProgressQueryReturn3002>
                  <SerialNo>SN300200000000000000000000002A</SerialNo>
                  <SendNodeCode>A1000143000104</SendNodeCode>
                  <DesNodeCode>A1000142000001</DesNodeCode>
                  <hxqyName>核心企业测试有限公司</hxqyName>
                  <hxqyCode>91110000ABCDEFGH12</hxqyCode>
                  <QueryType>1</QueryType>
                  <QueryKey>BIZ20260513000002</QueryKey>
                  <ReturnCode>01</ReturnCode>
                </ProgressQueryReturn3002>
              </MSG>
            </CFX>
            """;

    private static final String INVALID_MISSING_RETURNCODE_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <CFX>
              <HEAD>
                <Version>1.0</Version>
                <SrcNode>A1000143000104</SrcNode>
                <DesNode>A1000142000001</DesNode>
                <App>FEPx</App>
                <MsgNo>3002</MsgNo>
                <MsgId>30020000000000000003</MsgId>
                <CorrMsgId>30010000000000000003</CorrMsgId>
                <WorkDate>20260513</WorkDate>
              </HEAD>
              <MSG>
                <RealHead3002>
                  <SendOrgCode>30500000000000</SendOrgCode>
                  <EntrustDate>20260513</EntrustDate>
                  <TransitionNo>00000003</TransitionNo>
                  <Result>10000</Result>
                </RealHead3002>
                <ProgressQueryReturn3002>
                  <SerialNo>SN300200000000000000000000003A</SerialNo>
                  <SendNodeCode>A1000143000104</SendNodeCode>
                  <DesNodeCode>A1000142000001</DesNodeCode>
                  <hxqyName>核心企业测试有限公司</hxqyName>
                  <hxqyCode>91110000ABCDEFGH12</hxqyCode>
                  <QueryType>1</QueryType>
                  <QueryKey>BIZ20260513000003</QueryKey>
                </ProgressQueryReturn3002>
              </MSG>
            </CFX>
            """;

    @Test
    void valid3002FullFields_shouldPass() {
        ValidationResult result = validator.validate(MessageType.MSG_3002,
                VALID_FULL_FIELDS_XML.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void valid3002OptionalOmitted_shouldPass() {
        ValidationResult result = validator.validate(MessageType.MSG_3002,
                VALID_OPTIONAL_OMITTED_XML.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void invalid3002_missingReturnCode_shouldFail() {
        ValidationResult result = validator.validate(MessageType.MSG_3002,
                INVALID_MISSING_RETURNCODE_XML.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid()).isFalse();
        assertThat(result.errors())
                .as("error must reference missing ReturnCode field")
                .isNotEmpty()
                .anyMatch(e -> e.contains("ReturnCode"));
    }
}
