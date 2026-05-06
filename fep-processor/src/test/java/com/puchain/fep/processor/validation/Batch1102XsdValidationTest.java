package com.puchain.fep.processor.validation;

import com.puchain.fep.converter.type.MessageType;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BATCH 1102 报文 XSD schema 校验测试（沿用 Batch1103XsdValidationTest 模式 —
 * 直构造 XsdValidator + XsdSchemaRegistry，无 Spring 容器依赖）。
 *
 * <p>覆盖 valid + 缺必填字段（ItemId）两个场景。HEAD 字段以 Base.xsd 实测 sequence 为准。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class Batch1102XsdValidationTest extends AbstractXsdValidationTest {

    @Test
    void valid1102Sample_shouldPass() {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <CFX>
                  <HEAD>
                    <Version>1.0</Version>
                    <SrcNode>12345678901234</SrcNode>
                    <DesNode>A1000143000104</DesNode>
                    <App>HNDEMP</App>
                    <MsgNo>1102</MsgNo>
                    <MsgId>20260505120000000001</MsgId>
                    <CorrMsgId>20260505120000000001</CorrMsgId>
                    <WorkDate>20260505</WorkDate>
                  </HEAD>
                  <MSG>
                    <BatchHead1102>
                      <SendOrgCode>12345678901234</SendOrgCode>
                      <EntrustDate>20260505</EntrustDate>
                      <TransitionNo>00000001</TransitionNo>
                    </BatchHead1102>
                    <DataTransferCheckRequest1102>
                      <DataTransferCheck>
                        <ItemId>1</ItemId>
                        <MainClass>MainA01</MainClass>
                        <SecondClass>SubA0101</SecondClass>
                        <Period>01</Period>
                        <FileDate>20260505</FileDate>
                      </DataTransferCheck>
                    </DataTransferCheckRequest1102>
                  </MSG>
                </CFX>
                """;
        ValidationResult result = validator.validate(MessageType.MSG_1102,
                xml.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void invalid1102_missingItemId_shouldFail() {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <CFX>
                  <HEAD>
                    <Version>1.0</Version><SrcNode>12345678901234</SrcNode>
                    <DesNode>A1000143000104</DesNode><App>HNDEMP</App>
                    <MsgNo>1102</MsgNo><MsgId>20260505120000000001</MsgId>
                    <CorrMsgId>20260505120000000001</CorrMsgId><WorkDate>20260505</WorkDate>
                  </HEAD>
                  <MSG>
                    <BatchHead1102>
                      <SendOrgCode>12345678901234</SendOrgCode>
                      <EntrustDate>20260505</EntrustDate>
                      <TransitionNo>00000001</TransitionNo>
                    </BatchHead1102>
                    <DataTransferCheckRequest1102>
                      <DataTransferCheck>
                        <MainClass>MainA01</MainClass>
                        <SecondClass>SubA0101</SecondClass>
                        <Period>01</Period>
                        <FileDate>20260505</FileDate>
                      </DataTransferCheck>
                    </DataTransferCheckRequest1102>
                  </MSG>
                </CFX>
                """;
        ValidationResult result = validator.validate(MessageType.MSG_1102,
                xml.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid()).isFalse();
        assertThat(result.errors())
                .as("error must reference missing ItemId field, not unrelated XSD violation")
                .isNotEmpty()
                .anyMatch(e -> e.contains("ItemId"));
    }
}
