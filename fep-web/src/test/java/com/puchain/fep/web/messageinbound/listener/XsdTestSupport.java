package com.puchain.fep.web.messageinbound.listener;

/**
 * Shared XSD-related test fixture helpers for inbound wire integration tests.
 *
 * <p>Currently exposes {@link #pad30(String)} — padding/truncating raw business identifiers
 * to satisfy the {@code DataType.xsd} {@code SerialNo simpleType} {@code <xsd:length value="30"/>}
 * fixed-length constraint (派生 Text base, lines 616-623 in {@code DataType.xsd}). Required for
 * {@code SerialNo} fields across inbound listener tests when 真 {@code XsdValidator}
 * is wired (mock removed during R-NEW-1 真 XsdValidator 5 测试改造闭环, commits
 * {@code c8585e2} Inbound3112WireTest + {@code 04e8041} InboundAck9120BatchWireTest).
 *
 * <p>Extracted under Rule-of-2 (2 byte-identical copies in sibling test classes) on
 * 2026-05-28 to avoid a 3rd copy when future inbound wire tests (Inbound3105/3009/3103/3113
 * mirror-expansion) reproduce the same fixed-length SerialNo requirement.
 */
final class XsdTestSupport {

    private XsdTestSupport() {
        // utility class, no instantiation
    }

    /**
     * Pads the input to exactly 30 characters by appending {@code '0'} on the right;
     * truncates to 30 characters if the input is longer. Satisfies the
     * {@code DataType.xsd} {@code SerialNo simpleType} fixed length=30 constraint
     * (lines 616-623).
     *
     * @param raw raw input string (never {@code null}; behavior on {@code null} is to throw NPE
     *            via {@code .length()} which is acceptable for test fixtures)
     * @return a 30-character string, never {@code null}
     */
    public static String pad30(final String raw) {
        final int pad = 30 - raw.length();
        if (pad <= 0) {
            return raw.substring(0, 30);
        }
        return raw + "0".repeat(pad);
    }
}
