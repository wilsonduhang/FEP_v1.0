package com.puchain.fep.processor.body.supplychain;

import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.validation.AbstractXsdValidationTest;
import com.puchain.fep.processor.validation.ValidationResult;
import com.puchain.fep.processor.validation.XsdValidator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * P4-MSG-B T4 — 3000 报文 XSD 结构校验测试（电子凭证信息报送）。
 *
 * <p>验证 {@link DzpzInfo3000} 对应的完整 CFX envelope（HEAD + RealHead3000 + dzpzInfo3000）
 * 在 {@link XsdValidator} 下的合法 / 缺必填两个核心场景。复用
 * {@link AbstractXsdValidationTest#SHARED_VALIDATOR}（模块级共享 stateless 实例，
 * 节省 XSD schema 编译开销），无 Spring 容器依赖。</p>
 *
 * <p>3000.xsd 定义 dzpzInfo3000 sequence: SerialNo, SendNodeCode, DesNodeCode, ApplyMode,
 * pzInfo?, ExtInfo? — 前 4 个为必填，后 2 个 minOccurs=0。</p>
 *
 * <p>命名说明：原 Plan B v0.4 T4 约定文件名为 {@code DzpzInfo3000XsdIT}，但 surefire
 * 默认 include 仅含 {@code *Test.java}（red line {@code DEFECT-002} 静默跳过 {@code *IT.java}），
 * 故按 sibling {@code Batch1102XsdValidationTest} 实测命名约定改为 {@code XsdValidationTest}
 * 后缀，确保 {@code mvn test} 实际执行。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class DzpzInfo3000XsdValidationTest {

    private static XsdValidator validator;

    @BeforeAll
    static void initValidator() {
        validator = AbstractXsdValidationTest.SHARED_VALIDATOR;
    }

    @Test
    void valid3000_shouldPassValidation() {
        String xml = AbstractXsdValidationTest.wrapCfxTemplate(
                "12345678901234", "A1000143000104", "HNDEMP", "3000",
                "20260508120000000001", "20260508120000000001", "20260508", """
                <RealHead3000>
                  <SendOrgCode>12345678901234</SendOrgCode>
                  <EntrustDate>20260508</EntrustDate>
                  <TransitionNo>00000001</TransitionNo>
                </RealHead3000>
                <dzpzInfo3000>
                  <SerialNo>SN2026050800000000000000000001</SerialNo>
                  <SendNodeCode>12345678901234</SendNodeCode>
                  <DesNodeCode>A1000143000104</DesNodeCode>
                  <ApplyMode>01</ApplyMode>
                </dzpzInfo3000>""");

        ValidationResult result = validator.validate(MessageType.MSG_3000,
                xml.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid())
                .as("3000 valid sample errors=%s", result.errors())
                .isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void invalid3000_missingApplyMode_shouldFailValidation() {
        // ApplyMode 是 dzpzInfo3000 sequence 中的必填字段，移除应触发 XSD violation
        String xml = AbstractXsdValidationTest.wrapCfxTemplate(
                "12345678901234", "A1000143000104", "HNDEMP", "3000",
                "20260508120000000002", "20260508120000000002", "20260508", """
                <RealHead3000>
                  <SendOrgCode>12345678901234</SendOrgCode>
                  <EntrustDate>20260508</EntrustDate>
                  <TransitionNo>00000002</TransitionNo>
                </RealHead3000>
                <dzpzInfo3000>
                  <SerialNo>SN2026050800000000000000000002</SerialNo>
                  <SendNodeCode>12345678901234</SendNodeCode>
                  <DesNodeCode>A1000143000104</DesNodeCode>
                </dzpzInfo3000>""");

        ValidationResult result = validator.validate(MessageType.MSG_3000,
                xml.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid())
                .as("3000 missing ApplyMode must fail XSD validation")
                .isFalse();
        assertThat(result.errors()).isNotEmpty();
    }
}
