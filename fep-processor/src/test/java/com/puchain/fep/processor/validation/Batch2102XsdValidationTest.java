package com.puchain.fep.processor.validation;

import com.puchain.fep.converter.type.MessageType;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class Batch2102XsdValidationTest extends AbstractXsdValidationTest {

    @Test
    void valid2102Sample_shouldPass() {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <CFX>
                  <HEAD>
                    <Version>1.0</Version>
                    <SrcNode>A1000143000104</SrcNode>
                    <DesNode>12345678901234</DesNode>
                    <App>HNDEMP</App>
                    <MsgNo>2102</MsgNo>
                    <MsgId>20260505120000000001</MsgId>
                    <CorrMsgId>20260505120000000001</CorrMsgId>
                    <WorkDate>20260505</WorkDate>
                  </HEAD>
                  <MSG>
                    <BatchHead2102>
                      <SendOrgCode>A1000143000104</SendOrgCode>
                      <EntrustDate>20260505</EntrustDate>
                      <TransitionNo>00000003</TransitionNo>
                      <Result>00000</Result>
                    </BatchHead2102>
                    <DataTransferCheckResponse2102>
                      <DataTransferResult>
                        <ItemId>1</ItemId>
                        <MainClass>MainA01</MainClass>
                        <SecondClass>SubA0101</SecondClass>
                        <Period>01</Period>
                        <FileDate>20260505</FileDate>
                        <Status>01</Status>
                      </DataTransferResult>
                    </DataTransferCheckResponse2102>
                  </MSG>
                </CFX>
                """;
        ValidationResult result = validator.validate(MessageType.MSG_2102,
                xml.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void invalid2102_missingStatus_shouldFail() {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <CFX>
                  <HEAD>
                    <Version>1.0</Version><SrcNode>A1000143000104</SrcNode>
                    <DesNode>12345678901234</DesNode><App>HNDEMP</App>
                    <MsgNo>2102</MsgNo><MsgId>20260505120000000001</MsgId>
                    <CorrMsgId>20260505120000000001</CorrMsgId><WorkDate>20260505</WorkDate>
                  </HEAD>
                  <MSG>
                    <BatchHead2102>
                      <SendOrgCode>A1000143000104</SendOrgCode>
                      <EntrustDate>20260505</EntrustDate>
                      <TransitionNo>00000003</TransitionNo>
                      <Result>00000</Result>
                    </BatchHead2102>
                    <DataTransferCheckResponse2102>
                      <DataTransferResult>
                        <ItemId>1</ItemId>
                        <MainClass>MainA01</MainClass>
                        <SecondClass>SubA0101</SecondClass>
                        <Period>01</Period>
                        <FileDate>20260505</FileDate>
                      </DataTransferResult>
                    </DataTransferCheckResponse2102>
                  </MSG>
                </CFX>
                """;
        ValidationResult result = validator.validate(MessageType.MSG_2102,
                xml.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid()).isFalse();
        assertThat(result.errors())
                .as("error must reference missing Status field, not unrelated XSD violation")
                .isNotEmpty()
                .anyMatch(e -> e.contains("Status"));
    }
}
