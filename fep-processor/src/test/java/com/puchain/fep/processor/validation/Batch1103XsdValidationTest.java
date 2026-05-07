package com.puchain.fep.processor.validation;

import com.puchain.fep.common.util.FepConstants;

import com.puchain.fep.converter.type.MessageType;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BATCH 1103 报文 XSD schema 校验测试（沿用 SupplyChainXsdValidationTest 模式 —
 * 直构造 XsdValidator + XsdSchemaRegistry，无 Spring 容器依赖）。
 *
 * <p>覆盖 valid + 缺必填字段两个场景。HEAD 字段以 Base.xsd 实测 sequence 为准。</p>
 *
 * <p>R-2 (2026-05-07): 文本块内嵌入字面量 "A1000143000104" 是 HNDEMP 中心节点代码 fixture，与
 * {@link com.puchain.fep.common.util.FepConstants#HNDEMP_NODE_CODE} 同源。Java 文本块语法
 * (JEP 378) 不支持中段插入常量引用，故保留字面量于 fixture XML；新写测试请 import
 * {@code FepConstants} 并仅在 Java 表达式上下文中引用。</p>
 */
class Batch1103XsdValidationTest extends AbstractXsdValidationTest {

    @Test
    void valid1103Sample_shouldPass() {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <CFX>
                  <HEAD>
                    <Version>1.0</Version>
                    <SrcNode>12345678901234</SrcNode>
                    <DesNode>A1000143000104</DesNode>
                    <App>HNDEMP</App>
                    <MsgNo>1103</MsgNo>
                    <MsgId>20260505120000000001</MsgId>
                    <CorrMsgId>20260505120000000001</CorrMsgId>
                    <WorkDate>20260505</WorkDate>
                  </HEAD>
                  <MSG>
                    <BatchHead1103>
                      <SendOrgCode>12345678901234</SendOrgCode>
                      <EntrustDate>20260505</EntrustDate>
                      <TransitionNo>00000001</TransitionNo>
                    </BatchHead1103>
                    <CompanyInfoBatchRequest1103>
                      <CompanyInfoRequest>
                        <ItemId>1</ItemId>
                        <CompanyName>湖南示例实业有限公司</CompanyName>
                        <CompanyCode>91430100MA4L5XXXX1</CompanyCode>
                        <MainClass>MainA01</MainClass>
                        <SecondClass>SubA0101</SecondClass>
                        <AuthNo>AUTH2026050500001</AuthNo>
                        <AuthOrgCode>12345678901234</AuthOrgCode>
                      </CompanyInfoRequest>
                    </CompanyInfoBatchRequest1103>
                  </MSG>
                </CFX>
                """;

        ValidationResult result = validator.validate(MessageType.MSG_1103,
                xml.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid()).as("XSD validation result on valid 1103: %s",
                result.errors()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void invalid1103_missingItemId_shouldFail() {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <CFX>
                  <HEAD>
                    <Version>1.0</Version>
                    <SrcNode>12345678901234</SrcNode>
                    <DesNode>A1000143000104</DesNode>
                    <App>HNDEMP</App>
                    <MsgNo>1103</MsgNo>
                    <MsgId>20260505120000000002</MsgId>
                    <CorrMsgId>20260505120000000002</CorrMsgId>
                    <WorkDate>20260505</WorkDate>
                  </HEAD>
                  <MSG>
                    <BatchHead1103>
                      <SendOrgCode>12345678901234</SendOrgCode>
                      <EntrustDate>20260505</EntrustDate>
                      <TransitionNo>00000002</TransitionNo>
                    </BatchHead1103>
                    <CompanyInfoBatchRequest1103>
                      <CompanyInfoRequest>
                        <CompanyName>缺 ItemId 测试</CompanyName>
                        <CompanyCode>91430100MA4L5XXXX2</CompanyCode>
                        <MainClass>MainA01</MainClass>
                        <SecondClass>SubA0101</SecondClass>
                        <AuthNo>AUTH-INVALID</AuthNo>
                        <AuthOrgCode>12345678901234</AuthOrgCode>
                      </CompanyInfoRequest>
                    </CompanyInfoBatchRequest1103>
                  </MSG>
                </CFX>
                """;

        ValidationResult result = validator.validate(MessageType.MSG_1103,
                xml.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid()).isFalse();
        assertThat(result.errors())
                .as("error must reference missing ItemId field, not unrelated XSD violation")
                .isNotEmpty()
                .anyMatch(e -> e.contains("ItemId"));
    }
}
