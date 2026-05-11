package com.puchain.fep.processor.validation;

import com.puchain.fep.common.util.FepConstants;
import com.puchain.fep.converter.type.MessageType;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * XSD-driven validation tests for the 2101 (inbound DataTransfer) message body.
 *
 * <p>Coverage:</p>
 * <ul>
 *     <li>Valid 2101 XML with all required fields passes schema validation</li>
 *     <li>Invalid 2101 XML missing required {@code FileDate} is rejected</li>
 *     <li>Invalid 2101 XML violating MainClass minLength=2 / SecondClass maxLength=16 /
 *         FileDate pattern is rejected
 *         (红线 {@code feedback_xsd_validation_gap} +
 *         {@code feedback_fixture_data_must_satisfy_xsd_constraints})</li>
 *     <li>{@link XsdSchemaRegistry} resolves {@link MessageType#MSG_2101}</li>
 * </ul>
 *
 * <p>R-2 (2026-05-07): 文本块内嵌入字面量 "A1000143000104" 是 HNDEMP 中心节点代码 fixture，与
 * {@link FepConstants#HNDEMP_NODE_CODE} 同源。Java 文本块语法 (JEP 378) 不支持中段插入常量引用，
 * 故保留字面量于 fixture XML；新写测试请 import {@code FepConstants} 并仅在 Java 表达式上下文中引用。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class DataTransfer2101XsdValidationTest extends AbstractXsdValidationTest {

    private static final String VALID_2101_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <CFX>
              <HEAD>
                <Version>1.0</Version>
                <SrcNode>A1000143000104</SrcNode>
                <DesNode>A1000142000001</DesNode>
                <App>HNDEMP</App>
                <MsgNo>2101</MsgNo>
                <MsgId>21010000000000000001</MsgId>
                <CorrMsgId>00000000000000000000</CorrMsgId>
                <WorkDate>20260509</WorkDate>
              </HEAD>
              <MSG>
                <BatchHead2101>
                  <SendOrgCode>00000000000000</SendOrgCode>
                  <EntrustDate>20260509</EntrustDate>
                  <TransitionNo>20260509</TransitionNo>
                </BatchHead2101>
                <DataTransfer2101>
                  <MainClass>LSDX</MainClass>
                  <SecondClass>LSDX01</SecondClass>
                  <Period>01</Period>
                  <Type>01</Type>
                  <FileDate>20260509</FileDate>
                </DataTransfer2101>
              </MSG>
            </CFX>
            """;

    private static final String INVALID_MISSING_FILEDATE_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <CFX>
              <HEAD>
                <Version>1.0</Version>
                <SrcNode>A1000143000104</SrcNode>
                <DesNode>A1000142000001</DesNode>
                <App>HNDEMP</App>
                <MsgNo>2101</MsgNo>
                <MsgId>21010000000000000002</MsgId>
                <CorrMsgId>00000000000000000000</CorrMsgId>
                <WorkDate>20260509</WorkDate>
              </HEAD>
              <MSG>
                <BatchHead2101>
                  <SendOrgCode>00000000000000</SendOrgCode>
                  <EntrustDate>20260509</EntrustDate>
                  <TransitionNo>20260509</TransitionNo>
                </BatchHead2101>
                <DataTransfer2101>
                  <MainClass>LSDX</MainClass>
                  <SecondClass>LSDX01</SecondClass>
                  <Period>01</Period>
                  <Type>01</Type>
                </DataTransfer2101>
              </MSG>
            </CFX>
            """;

    @Test
    void valid2101_shouldPass() {
        ValidationResult result = validator.validate(MessageType.MSG_2101,
                VALID_2101_XML.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void invalid2101_missingFileDate_shouldFail() {
        ValidationResult result = validator.validate(MessageType.MSG_2101,
                INVALID_MISSING_FILEDATE_XML.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid()).isFalse();
        assertThat(result.errors())
                .as("error must reference missing FileDate field")
                .isNotEmpty()
                .anyMatch(e -> e.contains("FileDate"));
    }

    @Test
    void invalid2101_mainClassShorterThanMinLength_shouldFail() {
        String xml = VALID_2101_XML.replace(
                "<MainClass>LSDX</MainClass>",
                "<MainClass>L</MainClass>");

        ValidationResult result = validator.validate(MessageType.MSG_2101,
                xml.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid()).isFalse();
        assertThat(result.errors())
                .as("error must reference MainClass minLength=2 violation")
                .isNotEmpty()
                .anyMatch(e -> e.contains("MainClass"));
    }

    @Test
    void invalid2101_secondClassLongerThanMaxLength_shouldFail() {
        String xml = VALID_2101_XML.replace(
                "<SecondClass>LSDX01</SecondClass>",
                "<SecondClass>LSDX0123456789ABZ</SecondClass>");

        ValidationResult result = validator.validate(MessageType.MSG_2101,
                xml.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid()).isFalse();
        assertThat(result.errors())
                .as("error must reference SecondClass maxLength=16 violation")
                .isNotEmpty()
                .anyMatch(e -> e.contains("SecondClass"));
    }

    @Test
    void invalid2101_fileDateNotMatchingPattern_shouldFail() {
        // XSD Date pattern: [0-9]{4}(0[1-9]|1[0-2])([0-2][1-9]|[12][0-9]|3[01]) — lexical only,
        // 不校验语义日期 (02/30 / 04/31 等会通过 pattern 校验)。negative case 必须用 lexical-detectable
        // 越界：month 13 不在 (0[1-9]|1[0-2]) 范围内，会被 pattern reject。
        String xml = VALID_2101_XML.replace(
                "<FileDate>20260509</FileDate>",
                "<FileDate>20261301</FileDate>");

        ValidationResult result = validator.validate(MessageType.MSG_2101,
                xml.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid()).isFalse();
        assertThat(result.errors())
                .as("error must reference FileDate pattern violation (invalid month 13)")
                .isNotEmpty()
                .anyMatch(e -> e.contains("FileDate"));
    }

    @Test
    void registry_should_supports_msg_2101() {
        assertThat(new XsdSchemaRegistry().schemaOf(MessageType.MSG_2101)).isNotNull();
    }
}
