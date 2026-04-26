package com.puchain.fep.processor.validation;

import com.puchain.fep.converter.type.MessageType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for XSD validation of supply chain extension message types
 * (3009 / 3020 / 3101 / 3102 / 3103 / 3105 / 3107 / 3108 / 3109 / 3112 / 3113 /
 * 3115 / 3116 / 3120) introduced in P2d-ext (Tasks 1-7).
 *
 * <p>Fourteen positive samples cover every newly supported message type, and three
 * negative samples (3101 / 3105 / 3109) drop the required {@code SerialNo}
 * element to verify XSD constraint enforcement. Naming intentionally avoids the
 * existing {@code SupplyChainXsdValidationTest} (which covers 3001-3006 + 9120)
 * so both classes can co-exist without test collisions.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class SupplyChainExtXsdValidationTest {

    private static final String SAMPLE_DIR = "/samples/";
    private static final String INVALID_DIR = "/samples/invalid/";

    private static XsdValidator validator;

    @BeforeAll
    static void init() {
        validator = new XsdValidator(new XsdSchemaRegistry());
    }

    /**
     * 14 valid samples for the P2d-ext supply chain extension scope (Tasks 1-7).
     */
    @ParameterizedTest(name = "valid {0} sample should pass XSD validation")
    @ValueSource(strings = {
            "3009", "3020",
            "3101", "3102", "3103", "3105", "3107", "3108", "3109",
            "3112", "3113", "3115", "3116", "3120"
    })
    void validSample_shouldPassValidation(final String msgNo) throws IOException {
        MessageType type = MessageType.byMsgNo(msgNo)
                .orElseThrow(() -> new IllegalStateException("Unknown msgNo: " + msgNo));
        byte[] xml = loadSample(SAMPLE_DIR + msgNo + "-valid.xml");

        ValidationResult result = validator.validate(type, xml);

        assertThat(result.valid())
                .as("validation errors for %s: %s", msgNo, result.errors())
                .isTrue();
        assertThat(result.errors()).isEmpty();
    }

    /**
     * 3 invalid samples — each removes the required {@code SerialNo} element
     * from the body. Schema MUST report a violation.
     */
    @ParameterizedTest(name = "invalid {0} (missing SerialNo) should fail XSD validation")
    @CsvSource({
            "3101, ContractInfo3101",
            "3105, rzApplyInfo3105",
            "3109, qyRegister3109"
    })
    void invalidSample_missingSerialNo_shouldFailValidation(
            final String msgNo, final String bodyElementName) throws IOException {
        MessageType type = MessageType.byMsgNo(msgNo)
                .orElseThrow(() -> new IllegalStateException("Unknown msgNo: " + msgNo));
        byte[] xml = loadSample(INVALID_DIR + msgNo + "-missing-required.xml");

        ValidationResult result = validator.validate(type, xml);

        assertThat(result.valid())
                .as("expected XSD validation to fail for %s body %s", msgNo, bodyElementName)
                .isFalse();
        assertThat(result.errors()).isNotEmpty();
    }

    /**
     * 从 test resources 加载报文样本文件为字节数组。
     *
     * @param path classpath 资源路径（相对 test/resources，形如 {@code /samples/3101-valid.xml}）
     * @return 样本文件完整字节内容
     * @throws IOException 当资源不存在或读取失败
     */
    private static byte[] loadSample(final String path) throws IOException {
        try (InputStream is = SupplyChainExtXsdValidationTest.class.getResourceAsStream(path)) {
            if (is == null) {
                throw new IOException("Sample not found: " + path);
            }
            return is.readAllBytes();
        }
    }
}
