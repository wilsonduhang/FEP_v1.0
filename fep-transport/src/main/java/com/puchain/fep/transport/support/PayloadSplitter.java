package com.puchain.fep.transport.support;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Large-message splitter for TLQ transport.
 *
 * <p>TLQ limits each message attribute to 8 KB. Three attributes (xmlstr, xmlstr1, xmlstr2)
 * give a combined maximum of 24 KB. This utility splits a payload string into up to three
 * parts by UTF-8 byte boundaries, ensuring that no multi-byte character is broken at a
 * split point.</p>
 *
 * <p>Refer to PRD §3.6 for the specification.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public final class PayloadSplitter {

    /** Maximum bytes per TLQ attribute. */
    public static final int MAX_PART_BYTES = 8192;

    /** Maximum total bytes across all three attributes. */
    public static final int MAX_TOTAL_BYTES = MAX_PART_BYTES * 3;

    /** UTF-8 continuation byte mask: top 2 bits are 10xxxxxx. */
    private static final int UTF8_CONTINUATION_MASK = 0xC0;

    /** UTF-8 continuation byte pattern: 10xxxxxx. */
    private static final int UTF8_CONTINUATION_PATTERN = 0x80;

    private PayloadSplitter() {
        // utility class
    }

    /**
     * Result of splitting a payload into up to three parts.
     *
     * @param xmlstr  first part (always non-null)
     * @param xmlstr1 second part, or {@code null} if payload fits in one part
     * @param xmlstr2 third part, or {@code null} if payload fits in two parts
     */
    public record SplitResult(String xmlstr, String xmlstr1, String xmlstr2) { }

    /**
     * Split the given payload into up to three TLQ attribute strings.
     *
     * <ul>
     *   <li>payload &le; 8192 bytes &rarr; xmlstr only</li>
     *   <li>8192 &lt; payload &le; 16384 bytes &rarr; xmlstr + xmlstr1</li>
     *   <li>16384 &lt; payload &le; 24576 bytes &rarr; xmlstr + xmlstr1 + xmlstr2</li>
     *   <li>payload &gt; 24576 bytes &rarr; throws {@link FepBusinessException}</li>
     * </ul>
     *
     * @param payload the payload string to split
     * @return split result with up to three parts
     * @throws FepBusinessException if payload exceeds 24 KB (TRANS_7001)
     */
    public static SplitResult split(final String payload) {
        Objects.requireNonNull(payload, "payload must not be null");
        if (payload.isEmpty()) {
            return new SplitResult("", null, null);
        }

        byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);

        if (bytes.length > MAX_TOTAL_BYTES) {
            throw new FepBusinessException(FepErrorCode.TRANS_7001,
                    "Payload size " + bytes.length + " bytes exceeds TLQ maximum " + MAX_TOTAL_BYTES + " bytes");
        }

        if (bytes.length <= MAX_PART_BYTES) {
            return new SplitResult(payload, null, null);
        }

        int cut1 = findUtf8SafeCut(bytes, MAX_PART_BYTES);
        String part1 = new String(bytes, 0, cut1, StandardCharsets.UTF_8);

        if (bytes.length <= MAX_PART_BYTES * 2) {
            String part2 = new String(bytes, cut1, bytes.length - cut1, StandardCharsets.UTF_8);
            return new SplitResult(part1, part2, null);
        }

        int cut2 = findUtf8SafeCut(bytes, cut1 + MAX_PART_BYTES);
        String part2 = new String(bytes, cut1, cut2 - cut1, StandardCharsets.UTF_8);
        String part3 = new String(bytes, cut2, bytes.length - cut2, StandardCharsets.UTF_8);
        return new SplitResult(part1, part2, part3);
    }

    /**
     * Reassemble up to three parts back into the original payload.
     *
     * @param xmlstr  first part
     * @param xmlstr1 second part (may be {@code null})
     * @param xmlstr2 third part (may be {@code null})
     * @return the reassembled payload
     */
    public static String reassemble(final String xmlstr, final String xmlstr1, final String xmlstr2) {
        String s1 = xmlstr != null ? xmlstr : "";
        String s2 = xmlstr1 != null ? xmlstr1 : "";
        String s3 = xmlstr2 != null ? xmlstr2 : "";
        return s1 + s2 + s3;
    }

    /**
     * Find a UTF-8 safe cut position at or before {@code targetPos}.
     *
     * <p>If {@code targetPos} falls on a UTF-8 continuation byte (0x80..0xBF), move backward
     * to the start byte of that character so the split does not break a multi-byte sequence.</p>
     *
     * @param bytes     the UTF-8 encoded byte array
     * @param targetPos the desired cut position
     * @return a safe cut position &le; targetPos that does not break a UTF-8 character
     */
    private static int findUtf8SafeCut(final byte[] bytes, final int targetPos) {
        if (targetPos >= bytes.length) {
            return bytes.length;
        }
        int pos = targetPos;
        // UTF-8 continuation bytes have the pattern 10xxxxxx (0x80..0xBF)
        while (pos > 0 && (bytes[pos] & UTF8_CONTINUATION_MASK) == UTF8_CONTINUATION_PATTERN) {
            pos--;
        }
        return pos;
    }
}
