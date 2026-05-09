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

class DataTransfer2101XsdValidationTest {

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
                  <SendOrgCode>0000000000000000</SendOrgCode>
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
                  <SendOrgCode>0000000000000000</SendOrgCode>
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

    private final XsdSchemaRegistry registry = new XsdSchemaRegistry();

    @Test
    void schema_should_validate_well_formed_2101_xml() throws Exception {
        Validator v = registry.schemaOf(MessageType.MSG_2101).newValidator();
        v.validate(new StreamSource(new ByteArrayInputStream(
                VALID_2101_XML.getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    void schema_should_reject_2101_xml_missing_required_file_date() {
        Validator v = registry.schemaOf(MessageType.MSG_2101).newValidator();
        assertThatThrownBy(() -> v.validate(new StreamSource(new ByteArrayInputStream(
                INVALID_MISSING_FILEDATE_XML.getBytes(StandardCharsets.UTF_8)))))
                .isInstanceOf(SAXParseException.class)
                .hasMessageContaining("FileDate");
    }

    @Test
    void registry_should_supports_msg_2101() {
        assertThat(registry.schemaOf(MessageType.MSG_2101)).isNotNull();
    }
}
