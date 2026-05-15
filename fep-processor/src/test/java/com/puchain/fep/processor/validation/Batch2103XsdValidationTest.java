package com.puchain.fep.processor.validation;

import com.puchain.fep.common.util.FepConstants;

import com.puchain.fep.converter.type.MessageType;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BATCH 2103 报文 XSD schema 校验测试（沿用 SupplyChainXsdValidationTest / Batch1103XsdValidationTest
 * 模式 — 直构造 XsdValidator + XsdSchemaRegistry，无 Spring 容器依赖）。
 *
 * <p>覆盖 valid + 缺必填字段（QueryResult）两个场景。HEAD 字段以 Base.xsd 实测 sequence 为准
 * （Version/SrcNode/DesNode/App/MsgNo/MsgId/CorrMsgId/WorkDate）。</p>
 *
 * <p>R-2 (2026-05-07): 文本块内嵌入字面量 "A1000143000104" 是 HNDEMP 中心节点代码 fixture，与
 * {@link com.puchain.fep.common.util.FepConstants#HNDEMP_NODE_CODE} 同源。Java 文本块语法
 * (JEP 378) 不支持中段插入常量引用，故保留字面量于 fixture XML；新写测试请 import
 * {@code FepConstants} 并仅在 Java 表达式上下文中引用。</p>
 */
class Batch2103XsdValidationTest extends AbstractXsdValidationTest {

    @Test
    void valid2103Sample_shouldPass() {
        String xml = wrapCfxTemplate(
                "A1000143000104", "12345678901234", "HNDEMP", "2103",
                "20260505120000000003", "20260505120000000001", "20260505", """
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
                </CompanyInfoBatchResponse2103>""");

        ValidationResult result = validator.validate(MessageType.MSG_2103,
                xml.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid()).as("XSD validation result on valid 2103: %s",
                result.errors()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void invalid2103_missingQueryResult_shouldFail() {
        String xml = wrapCfxTemplate(
                "A1000143000104", "12345678901234", "HNDEMP", "2103",
                "20260505120000000004", "20260505120000000002", "20260505", """
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
                </CompanyInfoBatchResponse2103>""");

        ValidationResult result = validator.validate(MessageType.MSG_2103,
                xml.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid()).isFalse();
        assertThat(result.errors())
                .as("error must reference missing QueryResult field, not unrelated XSD violation")
                .isNotEmpty()
                .anyMatch(e -> e.contains("QueryResult"));
    }
}
