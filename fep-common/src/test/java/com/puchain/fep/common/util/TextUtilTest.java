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
}
