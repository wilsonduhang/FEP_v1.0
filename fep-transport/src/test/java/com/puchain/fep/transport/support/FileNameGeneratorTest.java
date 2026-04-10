package com.puchain.fep.transport.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link FileNameGenerator}.
 *
 * @author FEP Team
 * @since 1.0.0
 */
class FileNameGeneratorTest {

    @Test
    void generate_normalCase_shouldFormatCorrectly() {
        String result = FileNameGenerator.generate("B1234567890123", "GYL", "HX01", "20260410", 1, null, "xml");

        assertThat(result).isEqualTo("B1234567890123_GYL_HX01_20260410_00000001.xml");
    }

    @Test
    void generate_withRetransmitNo_shouldIncludeRetransmitSuffix() {
        String result = FileNameGenerator.generate("B1234567890123", "GYL", "HX01", "20260410", 1, 2, "xml");

        assertThat(result).isEqualTo("B1234567890123_GYL_HX01_20260410_00000001_0002.xml");
    }

    @Test
    void generate_largeSeqNo_shouldZeroPadTo8Digits() {
        String result = FileNameGenerator.generate("B1234567890123", "COINFO", "I1001", "20260410", 99999, null, "csv");

        assertThat(result).isEqualTo("B1234567890123_COINFO_I1001_20260410_00099999.csv");
    }

    @Test
    void generate_nullInstitutionCode_shouldThrowNpe() {
        assertThatThrownBy(() -> FileNameGenerator.generate(null, "GYL", "HX01", "20260410", 1, null, "xml"))
                .isInstanceOf(NullPointerException.class);
    }
}
