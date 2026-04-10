package com.puchain.fep.converter.compress;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.converter.exception.MessageConverterException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ZipBase64Compressor}.
 */
class ZipBase64CompressorTest {

    private final ZipBase64Compressor compressor = new ZipBase64Compressor();

    @Test
    void roundTrip_shouldPreserveChineseContent() {
        final String original = "报文压缩测试内容-abc-".repeat(50);
        final String compressed = compressor.compress(original);
        assertThat(compressor.decompress(compressed)).isEqualTo(original);
    }

    @Test
    void compress_shouldShrinkRepetitiveContent() {
        final String original = "x".repeat(1000);
        final String compressed = compressor.compress(original);
        final byte[] compressedBytes = Base64.getDecoder().decode(compressed);
        assertThat(compressedBytes.length).isLessThan(original.getBytes(StandardCharsets.UTF_8).length);
    }

    @Test
    void compress_shouldProduceValidBase64() {
        final String compressed = compressor.compress("hello");
        assertThat(compressed).matches("^[A-Za-z0-9+/]+=*$");
    }

    @Test
    void emptyString_shouldRoundTrip() {
        final String compressed = compressor.compress("");
        assertThat(compressor.decompress(compressed)).isEqualTo("");
    }

    @Test
    void decompressInvalidBase64_shouldRaiseConv8005() {
        assertThatThrownBy(() -> compressor.decompress("NOT_BASE64!@#"))
                .isInstanceOfSatisfying(MessageConverterException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(FepErrorCode.CONV_8005));
    }

    @Test
    void decompressCorruptedData_shouldRaiseConv8005() {
        final String junk = Base64.getEncoder().encodeToString(new byte[]{0x00, 0x01, 0x02, 0x03});
        assertThatThrownBy(() -> compressor.decompress(junk))
                .isInstanceOfSatisfying(MessageConverterException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(FepErrorCode.CONV_8005));
    }
}
