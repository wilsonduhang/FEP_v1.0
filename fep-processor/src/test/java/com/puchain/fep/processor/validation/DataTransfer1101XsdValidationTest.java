package com.puchain.fep.processor.validation;

import com.puchain.fep.common.util.FepConstants;
import com.puchain.fep.converter.type.MessageType;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * XSD-driven validation tests for the 1101 (outbound DataTransfer) message body.
 *
 * <p>Coverage:</p>
 * <ul>
 *     <li>Valid 1101 XML with all required fields passes schema validation</li>
 *     <li>Valid 1101 XML with optional {@code Parameters} omitted still passes</li>
 *     <li>Invalid 1101 XML missing required {@code MainClass} is rejected</li>
 *     <li>Invalid 1101 XML violating MainClass minLength=2 / SecondClass maxLength=16 /
 *         Parameters maxLength=2000 / FileDate pattern is rejected
 *         (红线 {@code feedback_xsd_validation_gap} +
 *         {@code feedback_fixture_data_must_satisfy_xsd_constraints})</li>
 *     <li>{@link XsdSchemaRegistry} resolves {@link MessageType#MSG_1101}</li>
 * </ul>
 *
 * <p>R-2 (2026-05-07): 文本块内嵌入字面量 "A1000143000104" 是 HNDEMP 中心节点代码 fixture，与
 * {@link FepConstants#HNDEMP_NODE_CODE} 同源。Java 文本块语法 (JEP 378) 不支持中段插入常量引用，
 * 故保留字面量于 fixture XML；新写测试请 import {@code FepConstants} 并仅在 Java 表达式上下文中引用。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class DataTransfer1101XsdValidationTest extends AbstractXsdValidationTest {

    private static final String VALID_FULL_FIELDS_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <CFX>
              <HEAD>
                <Version>1.0</Version>
                <SrcNode>A1000142000001</SrcNode>
                <DesNode>A1000143000104</DesNode>
                <App>FEPx</App>
                <MsgNo>1101</MsgNo>
                <MsgId>11010000000000000001</MsgId>
                <CorrMsgId>00000000000000000000</CorrMsgId>
                <WorkDate>20260509</WorkDate>
              </HEAD>
              <MSG>
                <BatchHead1101>
                  <SendOrgCode>30500000000000</SendOrgCode>
                  <EntrustDate>20260509</EntrustDate>
                  <TransitionNo>20260509</TransitionNo>
                </BatchHead1101>
                <DataTransfer1101>
                  <MainClass>LSDX</MainClass>
                  <SecondClass>LSDX01</SecondClass>
                  <Period>01</Period>
                  <Type>01</Type>
                  <FileDate>20260509</FileDate>
                  <Parameters>k1=v1</Parameters>
                </DataTransfer1101>
              </MSG>
            </CFX>
            """;

    private static final String VALID_OPTIONAL_OMITTED_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <CFX>
              <HEAD>
                <Version>1.0</Version>
                <SrcNode>A1000142000001</SrcNode>
                <DesNode>A1000143000104</DesNode>
                <App>FEPx</App>
                <MsgNo>1101</MsgNo>
                <MsgId>11010000000000000002</MsgId>
                <CorrMsgId>00000000000000000000</CorrMsgId>
                <WorkDate>20260509</WorkDate>
              </HEAD>
              <MSG>
                <BatchHead1101>
                  <SendOrgCode>30500000000000</SendOrgCode>
                  <EntrustDate>20260509</EntrustDate>
                  <TransitionNo>20260509</TransitionNo>
                </BatchHead1101>
                <DataTransfer1101>
                  <MainClass>YWTB</MainClass>
                  <SecondClass>YWTB01</SecondClass>
                  <Period>1</Period>
                  <Type>1</Type>
                  <FileDate>20260509</FileDate>
                </DataTransfer1101>
              </MSG>
            </CFX>
            """;

    private static final String INVALID_MISSING_MAINCLASS_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <CFX>
              <HEAD>
                <Version>1.0</Version>
                <SrcNode>A1000142000001</SrcNode>
                <DesNode>A1000143000104</DesNode>
                <App>FEPx</App>
                <MsgNo>1101</MsgNo>
                <MsgId>11010000000000000003</MsgId>
                <CorrMsgId>00000000000000000000</CorrMsgId>
                <WorkDate>20260509</WorkDate>
              </HEAD>
              <MSG>
                <BatchHead1101>
                  <SendOrgCode>30500000000000</SendOrgCode>
                  <EntrustDate>20260509</EntrustDate>
                  <TransitionNo>20260509</TransitionNo>
                </BatchHead1101>
                <DataTransfer1101>
                  <SecondClass>LSDX01</SecondClass>
                  <Period>01</Period>
                  <Type>01</Type>
                  <FileDate>20260509</FileDate>
                </DataTransfer1101>
              </MSG>
            </CFX>
            """;

    @Test
    void valid1101FullFields_shouldPass() {
        ValidationResult result = validator.validate(MessageType.MSG_1101,
                VALID_FULL_FIELDS_XML.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void valid1101OptionalParametersOmitted_shouldPass() {
        ValidationResult result = validator.validate(MessageType.MSG_1101,
                VALID_OPTIONAL_OMITTED_XML.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void invalid1101_missingMainClass_shouldFail() {
        ValidationResult result = validator.validate(MessageType.MSG_1101,
                INVALID_MISSING_MAINCLASS_XML.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid()).isFalse();
        assertThat(result.errors())
                .as("error must reference missing MainClass field")
                .isNotEmpty()
                .anyMatch(e -> e.contains("MainClass"));
    }

    @Test
    void invalid1101_mainClassShorterThanMinLength_shouldFail() {
        String xml = VALID_FULL_FIELDS_XML.replace(
                "<MainClass>LSDX</MainClass>",
                "<MainClass>L</MainClass>");

        ValidationResult result = validator.validate(MessageType.MSG_1101,
                xml.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid()).isFalse();
        assertThat(result.errors())
                .as("error must reference MainClass minLength=2 violation")
                .isNotEmpty()
                .anyMatch(e -> e.contains("MainClass"));
    }

    @Test
    void invalid1101_secondClassLongerThanMaxLength_shouldFail() {
        String xml = VALID_FULL_FIELDS_XML.replace(
                "<SecondClass>LSDX01</SecondClass>",
                "<SecondClass>LSDX0123456789ABZ</SecondClass>");

        ValidationResult result = validator.validate(MessageType.MSG_1101,
                xml.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid()).isFalse();
        assertThat(result.errors())
                .as("error must reference SecondClass maxLength=16 violation")
                .isNotEmpty()
                .anyMatch(e -> e.contains("SecondClass"));
    }

    @Test
    void invalid1101_parametersLongerThanMaxLength_shouldFail() {
        final String overflowParams = "k=" + "v".repeat(1999);
        String xml = VALID_FULL_FIELDS_XML.replace(
                "<Parameters>k1=v1</Parameters>",
                "<Parameters>" + overflowParams + "</Parameters>");

        ValidationResult result = validator.validate(MessageType.MSG_1101,
                xml.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid()).isFalse();
        assertThat(result.errors())
                .as("error must reference Parameters maxLength=2000 violation")
                .isNotEmpty()
                .anyMatch(e -> e.contains("Parameters"));
    }

    @Test
    void invalid1101_fileDateNotMatchingPattern_shouldFail() {
        String xml = VALID_FULL_FIELDS_XML.replace(
                "<FileDate>20260509</FileDate>",
                "<FileDate>20260230</FileDate>");

        ValidationResult result = validator.validate(MessageType.MSG_1101,
                xml.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid()).isFalse();
        assertThat(result.errors())
                .as("error must reference FileDate pattern violation (invalid calendar day 20260230)")
                .isNotEmpty()
                .anyMatch(e -> e.contains("FileDate"));
    }

    @Test
    void registry_should_supports_msg_1101() {
        assertThat(new XsdSchemaRegistry().schemaOf(MessageType.MSG_1101)).isNotNull();
    }
}
