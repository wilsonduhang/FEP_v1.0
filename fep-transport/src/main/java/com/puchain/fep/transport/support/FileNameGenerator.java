package com.puchain.fep.transport.support;

import java.util.Objects;

/**
 * Generates file names for data exchange with HNDEMP per PRD section 3.5.
 *
 * <p>File name format:
 * {@code {institutionCode}_{bizCategory}_{bizSubCategory}_{bizDate}_{dailySeqNo(8)}[_{retransmitNo(4)}].{extension}}</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public final class FileNameGenerator {

    private FileNameGenerator() {
        // utility class
    }

    /**
     * Generate a file name following PRD section 3.5 conventions.
     *
     * @param institutionCode the institution code
     * @param bizCategory     the business category (e.g. GYL, COINFO)
     * @param bizSubCategory  the business sub-category (e.g. HX01, I1001)
     * @param bizDate         the business date in yyyyMMdd format
     * @param dailySeqNo      the daily sequence number (zero-padded to 8 digits)
     * @param retransmitNo    the retransmit number (zero-padded to 4 digits), or {@code null} if not a retransmission
     * @param extension       the file extension without dot (e.g. xml, csv)
     * @return the generated file name
     * @throws NullPointerException if any required String parameter is null
     */
    public static String generate(final String institutionCode,
                                  final String bizCategory,
                                  final String bizSubCategory,
                                  final String bizDate,
                                  final int dailySeqNo,
                                  final Integer retransmitNo,
                                  final String extension) {
        Objects.requireNonNull(institutionCode, "institutionCode must not be null");
        Objects.requireNonNull(bizCategory, "bizCategory must not be null");
        Objects.requireNonNull(bizSubCategory, "bizSubCategory must not be null");
        Objects.requireNonNull(bizDate, "bizDate must not be null");
        Objects.requireNonNull(extension, "extension must not be null");

        StringBuilder sb = new StringBuilder();
        sb.append(institutionCode)
          .append('_').append(bizCategory)
          .append('_').append(bizSubCategory)
          .append('_').append(bizDate)
          .append('_').append(String.format("%08d", dailySeqNo));

        if (retransmitNo != null) {
            sb.append('_').append(String.format("%04d", retransmitNo));
        }

        sb.append('.').append(extension);
        return sb.toString();
    }
}
