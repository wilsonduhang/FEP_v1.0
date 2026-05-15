package com.puchain.fep.processor.validation;

import com.puchain.fep.converter.type.MessageType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for XSD validation of common messages
 * (9000 / 9006 / 9007 / 9008 / 9009 / 9020 / 9100).
 *
 * <p>Seven positive samples cover all seven common message types. Six use the
 * conventional {@code RealHead9XXX} business head, while 9100 uses the special
 * {@code BatchHead9100} head (of {@code RequestHead} type) — 9100 is the only
 * common message whose business head element name deviates from the
 * {@code RealHead} convention. One negative sample drops the required
 * {@code Password} element from 9006 to verify XSD constraint enforcement.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class CommonXsdValidationTest {

    private static final String SAMPLE_DIR = "/samples/";

    private static XsdValidator validator;

    @BeforeAll
    static void init() {
        validator = AbstractXsdValidationTest.SHARED_VALIDATOR;
    }

    @Test
    void valid9000_shouldPassValidation() throws IOException {
        assertValid(MessageType.MSG_9000, SAMPLE_DIR + "9000-valid.xml");
    }

    @Test
    void valid9006_shouldPassValidation() throws IOException {
        assertValid(MessageType.MSG_9006, SAMPLE_DIR + "9006-valid.xml");
    }

    @Test
    void valid9007_shouldPassValidation() throws IOException {
        assertValid(MessageType.MSG_9007, SAMPLE_DIR + "9007-valid.xml");
    }

    @Test
    void valid9008_shouldPassValidation() throws IOException {
        assertValid(MessageType.MSG_9008, SAMPLE_DIR + "9008-valid.xml");
    }

    @Test
    void valid9009_shouldPassValidation() throws IOException {
        assertValid(MessageType.MSG_9009, SAMPLE_DIR + "9009-valid.xml");
    }

    @Test
    void valid9020_shouldPassValidation() throws IOException {
        assertValid(MessageType.MSG_9020, SAMPLE_DIR + "9020-valid.xml");
    }

    @Test
    void valid9100_shouldPassValidation() throws IOException {
        assertValid(MessageType.MSG_9100, SAMPLE_DIR + "9100-valid.xml");
    }

    @Test
    void invalid9006MissingPassword_shouldFailValidation() throws IOException {
        byte[] xml = loadSample(SAMPLE_DIR + "9006-invalid-missing-password.xml");
        ValidationResult result = validator.validate(MessageType.MSG_9006, xml);
        assertThat(result.valid()).isFalse();
        assertThat(result.errors())
                .isNotEmpty()
                .anyMatch(e -> e.contains("Password"));
    }

    /**
     * 对给定报文类型加载 classpath XML 样本并断言 XSD 校验通过（errors 列表为空）。
     *
     * @param type 报文类型枚举（定位 XSD schema）
     * @param path classpath 资源路径（相对 test/resources/samples/）
     * @throws IOException 样本资源读取失败（文件不存在或流异常）
     */
    private static void assertValid(final MessageType type, final String path) throws IOException {
        byte[] xml = loadSample(path);
        ValidationResult result = validator.validate(type, xml);
        assertThat(result.valid())
                .as("validation errors: %s", result.errors())
                .isTrue();
        assertThat(result.errors()).isEmpty();
    }

    /**
     * 从 test resources 加载报文样本文件为字节数组。
     *
     * @param path classpath 资源路径（相对 test/resources，形如 {@code /samples/common/9000-valid.xml}）
     * @return 样本文件完整字节内容
     * @throws IOException 当资源不存在或读取失败
     */
    private static byte[] loadSample(final String path) throws IOException {
        try (InputStream is = CommonXsdValidationTest.class.getResourceAsStream(path)) {
            if (is == null) {
                throw new IOException("Sample not found: " + path);
            }
            return is.readAllBytes();
        }
    }
}
