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

    private static XsdValidator validator;

    @BeforeAll
    static void init() {
        validator = new XsdValidator(new XsdSchemaRegistry());
    }

    @Test
    void valid9000_shouldPassValidation() throws IOException {
        assertValid(MessageType.MSG_9000, "/samples/9000-valid.xml");
    }

    @Test
    void valid9006_shouldPassValidation() throws IOException {
        assertValid(MessageType.MSG_9006, "/samples/9006-valid.xml");
    }

    @Test
    void valid9007_shouldPassValidation() throws IOException {
        assertValid(MessageType.MSG_9007, "/samples/9007-valid.xml");
    }

    @Test
    void valid9008_shouldPassValidation() throws IOException {
        assertValid(MessageType.MSG_9008, "/samples/9008-valid.xml");
    }

    @Test
    void valid9009_shouldPassValidation() throws IOException {
        assertValid(MessageType.MSG_9009, "/samples/9009-valid.xml");
    }

    @Test
    void valid9020_shouldPassValidation() throws IOException {
        assertValid(MessageType.MSG_9020, "/samples/9020-valid.xml");
    }

    @Test
    void valid9100_shouldPassValidation() throws IOException {
        assertValid(MessageType.MSG_9100, "/samples/9100-valid.xml");
    }

    @Test
    void invalid9006MissingPassword_shouldFailValidation() throws IOException {
        byte[] xml = loadSample("/samples/9006-invalid-missing-password.xml");
        ValidationResult result = validator.validate(MessageType.MSG_9006, xml);
        assertThat(result.valid()).isFalse();
        assertThat(result.errors())
                .isNotEmpty()
                .anyMatch(e -> e.contains("Password"));
    }

    private static void assertValid(final MessageType type, final String path) throws IOException {
        byte[] xml = loadSample(path);
        ValidationResult result = validator.validate(type, xml);
        assertThat(result.valid())
                .as("validation errors: %s", result.errors())
                .isTrue();
        assertThat(result.errors()).isEmpty();
    }

    private static byte[] loadSample(final String path) throws IOException {
        try (InputStream is = CommonXsdValidationTest.class.getResourceAsStream(path)) {
            if (is == null) {
                throw new IOException("Sample not found: " + path);
            }
            return is.readAllBytes();
        }
    }
}
