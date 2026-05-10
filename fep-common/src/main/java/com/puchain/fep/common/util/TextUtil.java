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
     * Truncates a string to at most {@code max} characters (UTF-16 code units). Null-safe.
     *
     * <p>Char-based: this method counts UTF-16 code units, not Unicode code points.
     * A surrogate pair (e.g. U+1D11E musical G-clef, encoded as two chars) may be
     * split when {@code max} falls between its high and low surrogate, producing
     * a lone surrogate at the truncation boundary. Acceptable for diagnostic log
     * truncation (current call sites: TLQ result summaries, signature prefixes)
     * but not for user-facing display strings.
     *
     * <p>Validation order: {@code max} is validated <em>before</em> the null check.
     * Therefore {@code truncate(null, 0)} throws {@code IllegalArgumentException},
     * not return {@code null}. Rationale: caller bug (max &le; 0) is more severe
     * than a null input and should fail fast regardless of input.
     *
     * @param s   the input string (may be null)
     * @param max maximum length in chars, must be &gt; 0
     * @return null if input is null; the original string if its length &le; max;
     *         otherwise the first {@code max} chars (may end on a lone surrogate)
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
