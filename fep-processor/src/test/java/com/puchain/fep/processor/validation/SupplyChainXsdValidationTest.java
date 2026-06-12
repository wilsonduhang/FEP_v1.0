package com.puchain.fep.processor.validation;

import com.puchain.fep.common.util.FepConstants;

import com.puchain.fep.converter.type.MessageType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for XSD validation of supply chain message types (3001-3006)
 * and the generic acknowledgement (9120). Each positive test constructs a complete
 * CFX envelope with HEAD, RealHead/BatchHead, and Body elements that satisfy all
 * XSD type constraints. Negative tests verify that structural violations are caught.
 *
 * <p>All 10 tests are expected to complete within 3 seconds total.</p>
 *
 * <p>R-2 (2026-05-07): 文本块内嵌入字面量 "A1000143000104" 是 HNDEMP 中心节点代码 fixture，与
 * {@link com.puchain.fep.common.util.FepConstants#HNDEMP_NODE_CODE} 同源。Java 文本块语法
 * (JEP 378) 不支持中段插入常量引用，故保留字面量于 fixture XML；新写测试请 import
 * {@code FepConstants} 并仅在 Java 表达式上下文中引用。</p>
 */
class SupplyChainXsdValidationTest {

    private static XsdValidator validator;

    /** Shared HEAD template; callers replace {@code {{MSG_NO}}} with the actual message number. */
    private static final String HEAD_TEMPLATE = """
            <HEAD>
                <Version>1.0</Version>
                <SrcNode>12345678901234</SrcNode>
                <DesNode>A1000143000104</DesNode>
                <App>HNDEMP</App>
                <MsgNo>{{MSG_NO}}</MsgNo>
                <MsgId>20260417120000000001</MsgId>
                <CorrMsgId>20260417120000000001</CorrMsgId>
                <WorkDate>20260417</WorkDate>
            </HEAD>""";

    private static final String REQUEST_HEAD = """
                <SendOrgCode>12345678901234</SendOrgCode>
                <EntrustDate>20260417</EntrustDate>
                <TransitionNo>00000001</TransitionNo>""";

    private static final String RESPONSE_HEAD = """
                <SendOrgCode>12345678901234</SendOrgCode>
                <EntrustDate>20260417</EntrustDate>
                <TransitionNo>00000001</TransitionNo>
                <Result>90000</Result>""";

    /** 30-char SerialNo (Text, length=30). */
    private static final String SERIAL_NO = "SN2026041700000000000000000001";

    @BeforeAll
    static void init() {
        validator = AbstractXsdValidationTest.SHARED_VALIDATOR;
    }

    // ── positive samples ──────────────────────────────────────────────

    @Test
    void valid3001_shouldPassValidation() {
        String xml = cfx("3001", """
                <RealHead3001>
            """ + REQUEST_HEAD + """
                </RealHead3001>
                <ProgressQuery3001>
                    <SerialNo>""" + SERIAL_NO + """
            </SerialNo>
                    <SendNodeCode>12345678901234</SendNodeCode>
                    <DesNodeCode>A1000143000104</DesNodeCode>
                    <hxqyName>""" + "\u6D4B\u8BD5\u6838\u5FC3\u4F01\u4E1A" + """
            </hxqyName>
                    <hxqyCode>123456789012345678</hxqyCode>
                    <QueryType>1</QueryType>
                    <QueryKey>KEY001</QueryKey>
                </ProgressQuery3001>""");

        ValidationResult result = validator.validate(MessageType.MSG_3001, toBytes(xml));

        assertThat(result.valid())
                .as("validation errors: %s", result.errors())
                .isTrue();
    }

    @Test
    void valid3002_shouldPassValidation() {
        String xml = cfx("3002", """
                <RealHead3002>
            """ + RESPONSE_HEAD + """
                </RealHead3002>
                <ProgressQueryReturn3002>
                    <SerialNo>""" + SERIAL_NO + """
            </SerialNo>
                    <SendNodeCode>12345678901234</SendNodeCode>
                    <DesNodeCode>A1000143000104</DesNodeCode>
                    <hxqyName>""" + "\u6D4B\u8BD5\u6838\u5FC3\u4F01\u4E1A" + """
            </hxqyName>
                    <hxqyCode>123456789012345678</hxqyCode>
                    <QueryType>1</QueryType>
                    <QueryKey>KEY001</QueryKey>
                    <ReturnCode>01</ReturnCode>
                </ProgressQueryReturn3002>""");

        ValidationResult result = validator.validate(MessageType.MSG_3002, toBytes(xml));

        assertThat(result.valid())
                .as("validation errors: %s", result.errors())
                .isTrue();
    }

    @Test
    void valid3003_shouldPassValidation() {
        String xml = cfx("3003", """
                <RealHead3003>
            """ + REQUEST_HEAD + """
                </RealHead3003>
                <pzInfoQuery3003>
                    <SerialNo>""" + SERIAL_NO + """
            </SerialNo>
                    <SendNodeCode>12345678901234</SendNodeCode>
                    <DesNodeCode>A1000143000104</DesNodeCode>
                    <hxqyName>""" + "\u6D4B\u8BD5\u6838\u5FC3\u4F01\u4E1A" + """
            </hxqyName>
                    <hxqyCode>123456789012345678</hxqyCode>
                    <pzNo>PZ202604170001</pzNo>
                </pzInfoQuery3003>""");

        ValidationResult result = validator.validate(MessageType.MSG_3003, toBytes(xml));

        assertThat(result.valid())
                .as("validation errors: %s", result.errors())
                .isTrue();
    }

    @Test
    void valid3004_shouldPassValidation() {
        String xml = cfx("3004", """
                <RealHead3004>
            """ + RESPONSE_HEAD + """
                </RealHead3004>
                <pzInfoReturn3004>
                    <SerialNo>""" + SERIAL_NO + """
            </SerialNo>
                    <SendNodeCode>12345678901234</SendNodeCode>
                    <DesNodeCode>A1000143000104</DesNodeCode>
                    <hxqyName>""" + "\u6D4B\u8BD5\u6838\u5FC3\u4F01\u4E1A" + """
            </hxqyName>
                    <hxqyCode>123456789012345678</hxqyCode>
                    <pzNo>PZ202604170001</pzNo>
                    <pzState>1</pzState>
                    <pzrzState>1</pzrzState>
                    <pzrzStatusInfo>
                        <pzNo>PZ202604170001</pzNo>
                        <rzPhaseCode>01</rzPhaseCode>
                        <BankNodeCode>12345678901234</BankNodeCode>
                    </pzrzStatusInfo>
                    <zpzAllInfo>
                        <SerialNumber>1</SerialNumber>
                        <pzNo>PZ202604170001SUB1</pzNo>
                        <pzClass>01</pzClass>
                        <qyAssignName>""" + "\u8F6C\u8BA9\u65B9\u4F01\u4E1A" + """
            </qyAssignName>
                        <qyAssignCode>123456789012345678</qyAssignCode>
                        <qyRecvName>""" + "\u63A5\u6536\u65B9\u4F01\u4E1A" + """
            </qyRecvName>
                        <qyRecvCode>987654321098765432</qyRecvCode>
                        <Amt>1000.00</Amt>
                        <UpdateDate>20260417</UpdateDate>
                        <pzFunction>001</pzFunction>
                        <pzState>1</pzState>
                        <pzrzState>1</pzrzState>
                        <pzMajorNo>PZ202604170001</pzMajorNo>
                        <LoanAmt>500.00</LoanAmt>
                        <SubState>1</SubState>
                    </zpzAllInfo>
                </pzInfoReturn3004>""");

        ValidationResult result = validator.validate(MessageType.MSG_3004, toBytes(xml));

        assertThat(result.valid())
                .as("validation errors: %s", result.errors())
                .isTrue();
    }

    @Test
    void validate_3004_withPzFlowInfo_shouldSucceed() {
        String xml = cfx("3004", """
                <RealHead3004>
                    <SendOrgCode>A1000143000104</SendOrgCode>
                    <EntrustDate>20260421</EntrustDate>
                    <TransitionNo>00000001</TransitionNo>
                    <Result>90000</Result>
                </RealHead3004>
                <pzInfoReturn3004>
                    <SerialNo>""" + SERIAL_NO + """
            </SerialNo>
                    <SendNodeCode>A1000143000104</SendNodeCode>
                    <DesNodeCode>10000000000001</DesNodeCode>
                    <hxqyName>核心企业A</hxqyName>
                    <hxqyCode>91110000000000001X</hxqyCode>
                    <pzNo>PZ20260421000001</pzNo>
                    <pzState>01</pzState>
                    <pzrzState>01</pzrzState>
                    <pzrzStatusInfo>
                        <pzNo>PZ20260421000001</pzNo>
                        <rzPhaseCode>01</rzPhaseCode>
                        <BankNodeCode>10000000000001</BankNodeCode>
                    </pzrzStatusInfo>
                    <pzInfo>
                        <PlatShortName>PLATA</PlatShortName>
                        <PlatCode>91110000000000100X</PlatCode>
                        <ExternalPlat>01</ExternalPlat>
                        <hxqyName>核心企业A</hxqyName>
                        <hxqyCode>91110000000000001X</hxqyCode>
                        <pzNo>PZ20260421000001</pzNo>
                        <pzClass>01</pzClass>
                        <pzFunction>001</pzFunction>
                        <klzrfName>开立方B</klzrfName>
                        <klzrfCode>91110000000000002X</klzrfCode>
                        <jsqyName>接收企业C</jsqyName>
                        <jsqyCode>91110000000000003X</jsqyCode>
                        <jsqyPlatNo>PLATC00001</jsqyPlatNo>
                        <pzAmt>100000.00</pzAmt>
                        <pzStartDate>20260421</pzStartDate>
                        <pzEndDate>20260521</pzEndDate>
                        <pzState>01</pzState>
                        <pzrzState>01</pzrzState>
                        <pzFlowNum>1</pzFlowNum>
                        <pzFlowInfo>
                            <SerialNumber>1</SerialNumber>
                            <pzNo>PZ20260421000001</pzNo>
                            <PreNo>PZ20260420000001</PreNo>
                            <qyAssignName>转让方A</qyAssignName>
                            <qyAssignCode>91110000000000001X</qyAssignCode>
                            <qyRecvName>接收方B</qyRecvName>
                            <qyRecvCode>91110000000000002X</qyRecvCode>
                            <Amt>100000.00</Amt>
                            <UpdateDate>20260421</UpdateDate>
                        </pzFlowInfo>
                        <pzMemo>备注</pzMemo>
                    </pzInfo>
                </pzInfoReturn3004>""");

        ValidationResult result = validator.validate(MessageType.MSG_3004, toBytes(xml));

        assertThat(result.valid())
                .as("validation errors: %s", result.errors())
                .isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void valid3005_shouldPassValidation() {
        String xml = cfx("3005", """
                <RealHead3005>
            """ + REQUEST_HEAD + """
                </RealHead3005>
                <qyAccQuery3005>
                    <SerialNo>""" + SERIAL_NO + """
            </SerialNo>
                    <SendNodeCode>12345678901234</SendNodeCode>
                    <DesNodeCode>A1000143000104</DesNodeCode>
                    <qyAccName>""" + "\u6D4B\u8BD5\u4F01\u4E1A\u8D26\u6237" + """
            </qyAccName>
                    <qyAccCode>1234567890123456</qyAccCode>
                </qyAccQuery3005>""");

        ValidationResult result = validator.validate(MessageType.MSG_3005, toBytes(xml));

        assertThat(result.valid())
                .as("validation errors: %s", result.errors())
                .isTrue();
    }

    @Test
    void valid3006_shouldPassValidation() {
        String xml = cfx("3006", """
                <RealHead3006>
            """ + RESPONSE_HEAD + """
                </RealHead3006>
                <qyAccQueryReturn3006>
                    <SerialNo>""" + SERIAL_NO + """
            </SerialNo>
                    <SendNodeCode>12345678901234</SendNodeCode>
                    <DesNodeCode>A1000143000104</DesNodeCode>
                    <qyAccName>""" + "\u6D4B\u8BD5\u4F01\u4E1A\u8D26\u6237" + """
            </qyAccName>
                    <qyAccCode>1234567890123456</qyAccCode>
                    <AccReturnCode>01</AccReturnCode>
                </qyAccQueryReturn3006>""");

        ValidationResult result = validator.validate(MessageType.MSG_3006, toBytes(xml));

        assertThat(result.valid())
                .as("validation errors: %s", result.errors())
                .isTrue();
    }

    @Test
    void valid3007_shouldPassValidation() {
        String xml = cfx("3007", """
                <RealHead3007>
            """ + REQUEST_HEAD + """
                </RealHead3007>
                <InvoCheckQuery3007>
                    <SerialNo>""" + SERIAL_NO + """
            </SerialNo>
                    <SendNodeCode>12345678901234</SendNodeCode>
                    <DesNodeCode>A1000143000104</DesNodeCode>
                    <InvoCode>011001234567</InvoCode>
                    <InvoNum>044001234567890123</InvoNum>
                    <CheckCode>ABCDEF</CheckCode>
                    <InvoAmtTax>10000.00</InvoAmtTax>
                    <InvoAmt>9433.96</InvoAmt>
                    <InvoDate>20260417</InvoDate>
                    <ywKeyValue>BIZKEY-2026041700001</ywKeyValue>
                </InvoCheckQuery3007>""");

        ValidationResult result = validator.validate(MessageType.MSG_3007, toBytes(xml));

        assertThat(result.valid())
                .as("validation errors: %s", result.errors())
                .isTrue();
    }

    @Test
    void valid3008_shouldPassValidation() {
        String xml = cfx("3008", """
                <RealHead3008>
            """ + RESPONSE_HEAD + """
                </RealHead3008>
                <InvoCheckReturn3008>
                    <SerialNo>""" + SERIAL_NO + """
            </SerialNo>
                    <SendNodeCode>A1000143000104</SendNodeCode>
                    <DesNodeCode>12345678901234</DesNodeCode>
                    <InvoCheckReturnCode>0</InvoCheckReturnCode>
                    <InvoCheckReturnMemo>""" + "核验通过" + """
            </InvoCheckReturnMemo>
                    <kpName>""" + "销售方企业" + """
            </kpName>
                    <kpCode>91430000123456789X</kpCode>
                    <spName>""" + "采购方企业" + """
            </spName>
                    <spCode>91430000987654321Y</spCode>
                    <InvoCode>011001234567</InvoCode>
                    <InvoNum>044001234567890123</InvoNum>
                    <CheckCode>ABCDEF</CheckCode>
                    <InvoAmtTax>10000.00</InvoAmtTax>
                    <InvoAmt>9433.96</InvoAmt>
                    <InvoDate>20260417</InvoDate>
                    <InvoFilename>invoice-2026041700001.pdf</InvoFilename>
                </InvoCheckReturn3008>""");

        ValidationResult result = validator.validate(MessageType.MSG_3008, toBytes(xml));

        assertThat(result.valid())
                .as("validation errors: %s", result.errors())
                .isTrue();
    }

    @Test
    void valid9120_shouldPassValidation() {
        String xml = cfx("9120", """
                <BatchHead9120>
            """ + RESPONSE_HEAD + """
                </BatchHead9120>
                <MsgReturn9120>
                    <OriMsgNo>3001</OriMsgNo>
                </MsgReturn9120>""");

        ValidationResult result = validator.validate(MessageType.MSG_9120, toBytes(xml));

        assertThat(result.valid())
                .as("validation errors: %s", result.errors())
                .isTrue();
    }

    // ── negative samples ──────────────────────────────────────────────

    @Test
    void missing3001RequiredField_shouldFailValidation() {
        // Missing SerialNo which is required in ProgressQuery3001
        String xml = cfx("3001", """
                <RealHead3001>
            """ + REQUEST_HEAD + """
                </RealHead3001>
                <ProgressQuery3001>
                    <SendNodeCode>12345678901234</SendNodeCode>
                    <DesNodeCode>A1000143000104</DesNodeCode>
                    <hxqyName>""" + "\u6D4B\u8BD5\u6838\u5FC3\u4F01\u4E1A" + """
            </hxqyName>
                    <hxqyCode>123456789012345678</hxqyCode>
                    <QueryType>1</QueryType>
                    <QueryKey>KEY001</QueryKey>
                </ProgressQuery3001>""");

        ValidationResult result = validator.validate(MessageType.MSG_3001, toBytes(xml));

        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).isNotEmpty();
    }

    @Test
    void invalid3004NestedType_shouldFailValidation() {
        // pzrzStatusInfo is missing required child BankNodeCode
        String xml = cfx("3004", """
                <RealHead3004>
            """ + RESPONSE_HEAD + """
                </RealHead3004>
                <pzInfoReturn3004>
                    <SerialNo>""" + SERIAL_NO + """
            </SerialNo>
                    <SendNodeCode>12345678901234</SendNodeCode>
                    <DesNodeCode>A1000143000104</DesNodeCode>
                    <hxqyName>""" + "\u6D4B\u8BD5\u6838\u5FC3\u4F01\u4E1A" + """
            </hxqyName>
                    <hxqyCode>123456789012345678</hxqyCode>
                    <pzNo>PZ202604170001</pzNo>
                    <pzState>1</pzState>
                    <pzrzState>1</pzrzState>
                    <pzrzStatusInfo>
                        <pzNo>PZ202604170001</pzNo>
                        <rzPhaseCode>01</rzPhaseCode>
                    </pzrzStatusInfo>
                </pzInfoReturn3004>""");

        ValidationResult result = validator.validate(MessageType.MSG_3004, toBytes(xml));

        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).isNotEmpty();
    }

    @Test
    void tooLong3006Field_shouldFailValidation() {
        // AccReturnMemo type is String0to200; we supply 201 chars
        String tooLong = "A".repeat(201);
        String xml = cfx("3006", """
                <RealHead3006>
            """ + RESPONSE_HEAD + """
                </RealHead3006>
                <qyAccQueryReturn3006>
                    <SerialNo>""" + SERIAL_NO + """
            </SerialNo>
                    <SendNodeCode>12345678901234</SendNodeCode>
                    <DesNodeCode>A1000143000104</DesNodeCode>
                    <qyAccName>""" + "\u6D4B\u8BD5\u4F01\u4E1A\u8D26\u6237" + """
            </qyAccName>
                    <qyAccCode>1234567890123456</qyAccCode>
                    <AccReturnCode>01</AccReturnCode>
                    <AccReturnMemo>""" + tooLong + """
            </AccReturnMemo>
                </qyAccQueryReturn3006>""");

        ValidationResult result = validator.validate(MessageType.MSG_3006, toBytes(xml));

        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).isNotEmpty();
    }

    // ── helpers ────────────────────────────────────────────────────────

    /**
     * Wraps MSG content into a complete CFX envelope.
     *
     * @param msgNo      the 4-digit message number
     * @param msgContent XML content inside {@code <MSG>}
     * @return complete CFX XML string
     */
    private static String cfx(final String msgNo, final String msgContent) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<CFX>\n"
                + HEAD_TEMPLATE.replace("{{MSG_NO}}", msgNo) + "\n"
                + "    <MSG>\n" + msgContent + "\n    </MSG>\n</CFX>";
    }

    private static byte[] toBytes(final String xml) {
        return xml.getBytes(StandardCharsets.UTF_8);
    }
}
