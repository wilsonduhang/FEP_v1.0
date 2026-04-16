package com.puchain.fep.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for {@link LogSanitizer}.
 *
 * @author FEP Team
 * @since 1.0.0
 */
class LogSanitizerTest {

    // --- sanitize ---

    @Test
    void sanitize_nullReturnsEmpty() {
        assertEquals("", LogSanitizer.sanitize(null));
    }

    @Test
    void sanitize_replacesNewlines() {
        assertEquals("line1\\r\\nline2", LogSanitizer.sanitize("line1\r\nline2"));
    }

    @Test
    void sanitize_plainTextUnchanged() {
        assertEquals("hello world", LogSanitizer.sanitize("hello world"));
    }

    // --- maskUsci ---

    @Test
    void maskUsci_nullReturnsNA() {
        assertEquals("N/A", LogSanitizer.maskUsci(null));
    }

    @Test
    void maskUsci_blankReturnsNA() {
        assertEquals("N/A", LogSanitizer.maskUsci("   "));
    }

    @Test
    void maskUsci_shortStringUnchanged() {
        assertEquals("XK7A", LogSanitizer.maskUsci("XK7A"));
    }

    @Test
    void maskUsci_standard18CharMasked() {
        assertEquals("**************XK7A", LogSanitizer.maskUsci("91310000MA1K40XK7A"));
    }

    @Test
    void maskUsci_fiveCharShowsLastFour() {
        assertEquals("*BCDE", LogSanitizer.maskUsci("ABCDE"));
    }
}
