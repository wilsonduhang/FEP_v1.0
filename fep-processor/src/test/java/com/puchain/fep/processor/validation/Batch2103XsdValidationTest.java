package com.puchain.fep.processor.validation;

import com.puchain.fep.converter.type.MessageType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BATCH 2103 报文 XSD schema 校验测试（沿用 SupplyChainXsdValidationTest / Batch1103XsdValidationTest
 * 模式 — 直构造 XsdValidator + XsdSchemaRegistry，无 Spring 容器依赖）。
 *
 * <p>覆盖 valid + 缺必填字段（QueryResult）两个场景。HEAD 字段以 Base.xsd 实测 sequence 为准
 * （Version/SrcNode/DesNode/App/MsgNo/MsgId/CorrMsgId/WorkDate）。</p>
 */
class Batch2103XsdValidationTest {

    private static XsdValidator validator;

    @BeforeAll
    static void init() {
        validator = new XsdValidator(new XsdSchemaRegistry());
    }

    @Test
    void valid2103Sample_shouldPass() {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <CFX>
                  <HEAD>
                    <Version>1.0</Version>
                    <SrcNode>A1000143000104</SrcNode>
                    <DesNode>12345678901234</DesNode>
                    <App>HNDEMP</App>
                    <MsgNo>2103</MsgNo>
                    <MsgId>20260505120000000003</MsgId>
                    <CorrMsgId>20260505120000000001</CorrMsgId>
                    <WorkDate>20260505</WorkDate>
                  </HEAD>
                  <MSG>
                    <BatchHead2103>
                      <SendOrgCode>12345678901234</SendOrgCode>
                      <EntrustDate>20260505</EntrustDate>
                      <TransitionNo>00000003</TransitionNo>
                      <Result>00000</Result>
                    </BatchHead2103>
                    <CompanyInfoBatchResponse2103>
                      <CompanyInfo>
                        <ItemId>1</ItemId>
                        <CompanyName>湖南示例实业有限公司</CompanyName>
                        <CompanyCode>91430100MA4L5XXXX1</CompanyCode>
                        <MainClass>MainA01</MainClass>
                        <SecondClass>SubA0101</SecondClass>
                        <AuthOrgCode>12345678901234</AuthOrgCode>
                        <QueryResult>00000</QueryResult>
                      </CompanyInfo>
                    </CompanyInfoBatchResponse2103>
                  </MSG>
                </CFX>
                """;

        ValidationResult result = validator.validate(MessageType.MSG_2103,
                xml.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid()).as("XSD validation result on valid 2103: %s",
                result.errors()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void invalid2103_missingQueryResult_shouldFail() {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <CFX>
                  <HEAD>
                    <Version>1.0</Version>
                    <SrcNode>A1000143000104</SrcNode>
                    <DesNode>12345678901234</DesNode>
                    <App>HNDEMP</App>
                    <MsgNo>2103</MsgNo>
                    <MsgId>20260505120000000004</MsgId>
                    <CorrMsgId>20260505120000000002</CorrMsgId>
                    <WorkDate>20260505</WorkDate>
                  </HEAD>
                  <MSG>
                    <BatchHead2103>
                      <SendOrgCode>12345678901234</SendOrgCode>
                      <EntrustDate>20260505</EntrustDate>
                      <TransitionNo>00000004</TransitionNo>
                      <Result>00000</Result>
                    </BatchHead2103>
                    <CompanyInfoBatchResponse2103>
                      <CompanyInfo>
                        <ItemId>1</ItemId>
                        <CompanyName>缺 QueryResult 测试</CompanyName>
                        <CompanyCode>91430100MA4L5XXXX2</CompanyCode>
                        <MainClass>MainA01</MainClass>
                        <SecondClass>SubA0101</SecondClass>
                        <AuthOrgCode>12345678901234</AuthOrgCode>
                      </CompanyInfo>
                    </CompanyInfoBatchResponse2103>
                  </MSG>
                </CFX>
                """;

        ValidationResult result = validator.validate(MessageType.MSG_2103,
                xml.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid()).isFalse();
        assertThat(result.errors())
                .as("error must reference missing QueryResult field, not unrelated XSD violation")
                .isNotEmpty()
                .anyMatch(e -> e.contains("QueryResult"));
    }
}
