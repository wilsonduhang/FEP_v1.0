package com.puchain.fep.processor.validation;

import com.puchain.fep.common.util.FepConstants;

import com.puchain.fep.converter.type.MessageType;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BATCH 1104 报文 XSD schema 校验测试（沿用 Batch1103XsdValidationTest 模式 —
 * 直构造 XsdValidator + XsdSchemaRegistry，无 Spring 容器依赖）。
 *
 * <p>覆盖 valid + 缺必填字段（ItemId）两个场景。HEAD 字段以 Base.xsd 实测 sequence 为准。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 *
 * <p>R-2 (2026-05-07): 文本块内嵌入字面量 "A1000143000104" 是 HNDEMP 中心节点代码 fixture，与
 * {@link com.puchain.fep.common.util.FepConstants#HNDEMP_NODE_CODE} 同源。Java 文本块语法
 * (JEP 378) 不支持中段插入常量引用，故保留字面量于 fixture XML；新写测试请 import
 * {@code FepConstants} 并仅在 Java 表达式上下文中引用。</p>
 */
class Batch1104XsdValidationTest extends AbstractXsdValidationTest {

    @Test
    void valid1104Sample_shouldPass() {
        String xml = wrapCfxTemplate(
                "12345678901234", "A1000143000104", "HNDEMP", "1104",
                "20260505120000000001", "20260505120000000001", "20260505", """
                <BatchHead1104>
                  <SendOrgCode>12345678901234</SendOrgCode>
                  <EntrustDate>20260505</EntrustDate>
                  <TransitionNo>00000001</TransitionNo>
                </BatchHead1104>
                <CompanyAuthFileBatchTransfer1104>
                  <CompanyAuthFile>
                    <ItemId>1</ItemId>
                    <CompanyName>湖南示例实业有限公司</CompanyName>
                    <CompanyCode>91430100MA4L5XXXX1</CompanyCode>
                    <AuthBeginDate>20260101</AuthBeginDate>
                    <AuthEndDate>20261231</AuthEndDate>
                    <AuthNo>AUTH2026050500001</AuthNo>
                    <AuthOrgCode>12345678901234</AuthOrgCode>
                    <IsUpdate>0</IsUpdate>
                    <FileName>authfile_001.pdf</FileName>
                  </CompanyAuthFile>
                </CompanyAuthFileBatchTransfer1104>""");
        ValidationResult result = validator.validate(MessageType.MSG_1104,
                xml.getBytes(StandardCharsets.UTF_8));
        assertThat(result.valid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void invalid1104_missingItemId_shouldFail() {
        String xml = wrapCfxTemplate(
                "12345678901234", "A1000143000104", "HNDEMP", "1104",
                "20260505120000000001", "20260505120000000001", "20260505", """
                <BatchHead1104>
                  <SendOrgCode>12345678901234</SendOrgCode>
                  <EntrustDate>20260505</EntrustDate>
                  <TransitionNo>00000001</TransitionNo>
                </BatchHead1104>
                <CompanyAuthFileBatchTransfer1104>
                  <CompanyAuthFile>
                    <CompanyName>湖南示例实业有限公司</CompanyName>
                    <CompanyCode>91430100MA4L5XXXX1</CompanyCode>
                    <AuthBeginDate>20260101</AuthBeginDate>
                    <AuthEndDate>20261231</AuthEndDate>
                    <AuthNo>AUTH2026050500001</AuthNo>
                    <AuthOrgCode>12345678901234</AuthOrgCode>
                    <FileName>authfile_001.pdf</FileName>
                  </CompanyAuthFile>
                </CompanyAuthFileBatchTransfer1104>""");
        ValidationResult result = validator.validate(MessageType.MSG_1104,
                xml.getBytes(StandardCharsets.UTF_8));
        assertThat(result.valid()).isFalse();
        assertThat(result.errors())
                .as("error must reference missing ItemId field, not unrelated XSD violation")
                .isNotEmpty()
                .anyMatch(e -> e.contains("ItemId"));
    }
}
