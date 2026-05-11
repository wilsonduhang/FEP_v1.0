package com.puchain.fep.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link TextUtil}.
 *
 * @author FEP Team
 * @since 1.0.0
 */
class TextUtilTest {

    @Test
    void truncate_nullReturnsNull() {
        assertNull(TextUtil.truncate(null, 10));
    }

    @Test
    void truncate_belowMaxReturnsAsIs() {
        assertEquals("abc", TextUtil.truncate("abc", 10));
    }

    @Test
    void truncate_atMaxReturnsAsIs() {
        assertEquals("abcdefghij", TextUtil.truncate("abcdefghij", 10));
    }

    @Test
    void truncate_aboveMaxReturnsTruncated() {
        assertEquals("abcdefghij", TextUtil.truncate("abcdefghijklmn", 10));
    }

    @Test
    void truncate_emptyReturnsEmpty() {
        assertEquals("", TextUtil.truncate("", 10));
    }

    @Test
    void truncate_zeroMaxThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> TextUtil.truncate("abc", 0));
    }

    @Test
    void truncate_negativeMaxThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> TextUtil.truncate("abc", -1));
    }

    /**
     * Surrogate pair split: U+1D11E (musical G-clef) is encoded as 2 UTF-16 chars
     * (high surrogate 0xD834 + low surrogate 0xDD1E). Three glyphs occupy 6 chars.
     * Truncating to 1 char returns a lone high surrogate — char-based behavior
     * documented in {@link TextUtil#truncate}. Test fixes the contract so future
     * refactors don't silently switch to code-point semantics.
     */
    @Test
    void truncate_splitsSurrogatePair() {
        final String threeGClefs = "𝄞𝄞𝄞";
        assertEquals(6, threeGClefs.length(),
                "three U+1D11E surrogate pairs occupy 6 UTF-16 chars");

        final String result = TextUtil.truncate(threeGClefs, 1);
        assertEquals(1, result.length(), "truncate to 1 yields exactly 1 char");
        assertEquals('\uD834', result.charAt(0),
                "result is lone high surrogate (char-based, not code-point-aware)");
    }

    /**
     * Validation order: {@code max} is checked before null. Documents the deliberate
     * fail-fast on caller bug (max &le; 0) regardless of input.
     */
    @Test
    void truncate_zeroMaxWithNullStringThrowsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> TextUtil.truncate(null, 0));
    }
}
