package com.puchain.fep.processor.validation;

import com.puchain.fep.converter.type.MessageType;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXParseException;

import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Validator;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DataTransfer1101XsdValidationTest {

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

    private final XsdSchemaRegistry registry = new XsdSchemaRegistry();

    @Test
    void schema_should_validate_well_formed_1101_xml_with_all_fields() throws Exception {
        Validator v = registry.schemaOf(MessageType.MSG_1101).newValidator();
        v.validate(new StreamSource(new ByteArrayInputStream(
                VALID_FULL_FIELDS_XML.getBytes(StandardCharsets.UTF_8))));
        // 无异常即通过
    }

    @Test
    void schema_should_validate_1101_xml_with_optional_parameters_omitted() throws Exception {
        Validator v = registry.schemaOf(MessageType.MSG_1101).newValidator();
        v.validate(new StreamSource(new ByteArrayInputStream(
                VALID_OPTIONAL_OMITTED_XML.getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    void schema_should_reject_1101_xml_missing_required_main_class() {
        Validator v = registry.schemaOf(MessageType.MSG_1101).newValidator();
        assertThatThrownBy(() -> v.validate(new StreamSource(new ByteArrayInputStream(
                INVALID_MISSING_MAINCLASS_XML.getBytes(StandardCharsets.UTF_8)))))
                .isInstanceOf(SAXParseException.class)
                .hasMessageContaining("MainClass");
    }

    @Test
    void registry_should_supports_msg_1101() {
        assertThat(registry.schemaOf(MessageType.MSG_1101)).isNotNull();
    }
}
