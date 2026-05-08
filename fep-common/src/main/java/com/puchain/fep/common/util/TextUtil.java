package com.puchain.fep.common.util;

/**
 * Generic text manipulation helpers (truncate, etc.).
 *
 * @author FEP Team
 * @since 1.0.0
 */
public final class TextUtil {

    private TextUtil() { }

    /**
     * Truncates a string to at most {@code max} characters. Null-safe.
     *
     * @param s   the input string (may be null)
     * @param max maximum length, must be &gt; 0
     * @return null if input is null; the original string if its length &le; max;
     *         otherwise the first {@code max} characters
     * @throws IllegalArgumentException if {@code max} &le; 0
     */
    public static String truncate(final String s, final int max) {
        if (max <= 0) {
            throw new IllegalArgumentException("max must be > 0, got: " + max);
        }
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
