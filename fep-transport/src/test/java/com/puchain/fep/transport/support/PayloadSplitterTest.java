package com.puchain.fep.transport.support;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.transport.support.PayloadSplitter.SplitResult;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link PayloadSplitter}.
 *
 * @author FEP Team
 * @since 1.0.0
 */
class PayloadSplitterTest {

    @Test
    void split_smallPayload_shouldUseOnlyXmlstr() {
        String payload = "A".repeat(100);
        SplitResult result = PayloadSplitter.split(payload);

        assertThat(result.xmlstr()).isEqualTo(payload);
        assertThat(result.xmlstr1()).isNull();
        assertThat(result.xmlstr2()).isNull();
    }

    @Test
    void split_exactly8KB_shouldUseOnlyXmlstr() {
        String payload = "A".repeat(8192);
        SplitResult result = PayloadSplitter.split(payload);

        assertThat(result.xmlstr()).isEqualTo(payload);
        assertThat(result.xmlstr1()).isNull();
        assertThat(result.xmlstr2()).isNull();
    }

    @Test
    void split_12KB_shouldUseTwoParts() {
        String payload = "B".repeat(12000);
        SplitResult result = PayloadSplitter.split(payload);

        assertThat(result.xmlstr()).hasSize(8192);
        assertThat(result.xmlstr1()).hasSize(12000 - 8192);
        assertThat(result.xmlstr2()).isNull();
        assertThat(PayloadSplitter.reassemble(result.xmlstr(), result.xmlstr1(), result.xmlstr2()))
                .isEqualTo(payload);
    }

    @Test
    void split_20KB_shouldUseThreeParts() {
        String payload = "C".repeat(20000);
        SplitResult result = PayloadSplitter.split(payload);

        assertThat(result.xmlstr().getBytes(StandardCharsets.UTF_8).length)
                .isLessThanOrEqualTo(PayloadSplitter.MAX_PART_BYTES);
        assertThat(result.xmlstr1().getBytes(StandardCharsets.UTF_8).length)
                .isLessThanOrEqualTo(PayloadSplitter.MAX_PART_BYTES);
        assertThat(result.xmlstr2().getBytes(StandardCharsets.UTF_8).length)
                .isLessThanOrEqualTo(PayloadSplitter.MAX_PART_BYTES);
        assertThat(PayloadSplitter.reassemble(result.xmlstr(), result.xmlstr1(), result.xmlstr2()))
                .isEqualTo(payload);
    }

    @Test
    void split_over24KB_shouldThrowException() {
        String payload = "D".repeat(25000);
        assertThatThrownBy(() -> PayloadSplitter.split(payload))
                .isInstanceOf(FepBusinessException.class)
                .satisfies(ex -> assertThat(((FepBusinessException) ex).getErrorCode())
                        .isEqualTo(FepErrorCode.TRANS_7001));
    }

    @Test
    void split_utf8MultiByte_shouldNotBreakCharacter() {
        // '中' is 3 bytes in UTF-8. 8190 A's (1 byte each) + '中' = 8193 bytes.
        // The splitter must move the cut back so '中' is not broken.
        String payload = "A".repeat(8190) + "中" + "E".repeat(100);
        SplitResult result = PayloadSplitter.split(payload);

        // First part must not exceed 8192 bytes
        byte[] part1Bytes = result.xmlstr().getBytes(StandardCharsets.UTF_8);
        assertThat(part1Bytes.length).isLessThanOrEqualTo(PayloadSplitter.MAX_PART_BYTES);

        // The '中' character should not be broken — it goes to part 2
        assertThat(result.xmlstr()).hasSize(8190);
        assertThat(result.xmlstr1()).startsWith("中");

        // Round-trip must match
        assertThat(PayloadSplitter.reassemble(result.xmlstr(), result.xmlstr1(), result.xmlstr2()))
                .isEqualTo(payload);
    }

    @Test
    void reassemble_allThreeParts_shouldMatchOriginal() {
        String payload = "F".repeat(20000);
        SplitResult result = PayloadSplitter.split(payload);
        String reassembled = PayloadSplitter.reassemble(result.xmlstr(), result.xmlstr1(), result.xmlstr2());

        assertThat(reassembled).isEqualTo(payload);
    }

    @Test
    void reassemble_nullParts_shouldHandleGracefully() {
        String result = PayloadSplitter.reassemble("hello", null, null);
        assertThat(result).isEqualTo("hello");
    }

    @Test
    void split_emptyPayload_shouldReturnEmptyXmlstr() {
        SplitResult result = PayloadSplitter.split("");

        assertThat(result.xmlstr()).isEmpty();
        assertThat(result.xmlstr1()).isNull();
        assertThat(result.xmlstr2()).isNull();
    }
}
