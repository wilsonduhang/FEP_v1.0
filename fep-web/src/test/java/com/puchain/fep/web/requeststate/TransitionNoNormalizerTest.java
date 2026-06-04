package com.puchain.fep.web.requeststate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit behaviour for {@link TransitionNoNormalizer}.
 *
 * <p>归一 = 纯防御 trim（<strong>非 30→8 截断</strong>）。两侧 transitionNo 均为 8 位业务流水号
 * （outbound {@code RequestBusinessHead.setTransitionNo}/{@code AbstractRealHead} 强制 8 位数字；
 * inbound {@code InboundTransitionNoExtractor.extract} 读同一业务头并 trim），故归一只需去除潜在
 * 边界空白即可让两侧值字节相等，长度/格式校验留给调用方按 unmatched 处理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@DisplayName("TransitionNoNormalizer: 防御 trim（非截断）")
class TransitionNoNormalizerTest {

    @Test
    void canonical_trimsBoundaryWhitespace() {
        assertThat(TransitionNoNormalizer.canonical("  12345678  ")).isEqualTo("12345678");
        assertThat(TransitionNoNormalizer.canonical("\t12345678\n")).isEqualTo("12345678");
    }

    @Test
    void canonical_alreadyNormalized_isIdempotent() {
        final String value = "12345678";
        assertThat(TransitionNoNormalizer.canonical(value)).isEqualTo(value);
        assertThat(TransitionNoNormalizer.canonical(TransitionNoNormalizer.canonical(value)))
                .isEqualTo(value);
    }

    @Test
    void canonical_null_returnsNull() {
        assertThat(TransitionNoNormalizer.canonical(null)).isNull();
    }

    @Test
    void canonical_blank_returnsNull() {
        assertThat(TransitionNoNormalizer.canonical("")).isNull();
        assertThat(TransitionNoNormalizer.canonical("   ")).isNull();
    }

    @Test
    void canonical_nonEightDigit_returnsTrimmedValueUnchanged_noTruncateNoStrip() {
        // 防御 trim 不强转、不截断、不补位：长度/格式不匹配交调用方判 unmatched
        assertThat(TransitionNoNormalizer.canonical("  1234567890123456789012345678  "))
                .isEqualTo("1234567890123456789012345678");
        assertThat(TransitionNoNormalizer.canonical("  123  ")).isEqualTo("123");
        assertThat(TransitionNoNormalizer.canonical("  ABCDEFGH  ")).isEqualTo("ABCDEFGH");
    }
}
