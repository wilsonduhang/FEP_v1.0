package com.puchain.fep.processor.validation;

import com.puchain.fep.converter.type.MessageType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class XsdValidatorTest {

    private static XsdValidator validator;

    @BeforeAll
    static void init() {
        XsdSchemaRegistry registry = new XsdSchemaRegistry();
        validator = new XsdValidator(registry);
    }

    @Test
    void validate_shouldReturnOk_forValidSample() throws IOException {
        byte[] xml = loadSample("/samples/1001-valid.xml");
        ValidationResult result = validator.validate(MessageType.MSG_1001, xml);
        assertThat(result.valid())
                .as("validation errors: %s", result.errors())
                .isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void validate_shouldReportMissingRequiredField() throws IOException {
        byte[] xml = loadSample("/samples/1001-missing-company-name.xml");
        ValidationResult result = validator.validate(MessageType.MSG_1001, xml);
        assertThat(result.valid()).isFalse();
        assertThat(result.errors())
                .isNotEmpty()
                .anyMatch(e -> e.contains("CompanyName"));
    }

    @Test
    void validate_shouldReportInvalidDate() throws IOException {
        byte[] xml = loadSample("/samples/1001-invalid-date.xml");
        ValidationResult result = validator.validate(MessageType.MSG_1001, xml);
        assertThat(result.valid()).isFalse();
        assertThat(result.errors())
                .isNotEmpty()
                .anyMatch(e -> e.contains("BeginDate") || e.contains("20261340"));
    }

    @Test
    void validate_shouldRejectNullXml() {
        assertThatThrownBy(() -> validator.validate(MessageType.MSG_1001, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("xml must not be null");
    }

    @Test
    void validate_shouldRejectOversizedXml() {
        byte[] oversized = new byte[10 * 1024 * 1024 + 1];
        assertThatThrownBy(() -> validator.validate(MessageType.MSG_1001, oversized))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("exceeds");
    }

    @Test
    void validate_shouldHandleMalformedXml_fatalError() {
        byte[] malformed = "<CFX><HEAD>".getBytes(StandardCharsets.UTF_8);
        ValidationResult result = validator.validate(MessageType.MSG_1001, malformed);
        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).isNotEmpty();
    }

    private byte[] loadSample(final String path) throws IOException {
        try (InputStream is = XsdValidatorTest.class.getResourceAsStream(path)) {
            if (is == null) {
                throw new IOException("sample not found: " + path);
            }
            return is.readAllBytes();
        }
    }
}
