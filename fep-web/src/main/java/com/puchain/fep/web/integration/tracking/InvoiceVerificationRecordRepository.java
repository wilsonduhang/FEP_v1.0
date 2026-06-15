package com.puchain.fep.web.integration.tracking;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link InvoiceVerificationRecordEntity}
 * (§6.4.1 FR-DATA-DB-01).
 *
 * <p>Exposes the minimum query surface required by
 * {@code InvoiceVerificationTrackingService} (idempotent upsert by serial number)
 * and the future §5.9 report / ops layer (time-window listing + raw-code count).
 * Mirrors {@code ReconciliationRecordRepository}.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Repository
public interface InvoiceVerificationRecordRepository
        extends JpaRepository<InvoiceVerificationRecordEntity, String> {

    /**
     * Finds the unique record for a business serial number.
     * Backed by unique constraint {@code uq_invoice_verif_serial}; used by the
     * write path for idempotent upsert.
     *
     * @param serialNo business serial number
     * @return matching entity or empty
     */
    Optional<InvoiceVerificationRecordEntity> findBySerialNo(String serialNo);

    /**
     * Pages records whose verification time falls within {@code [from, to]}.
     *
     * @param from     inclusive lower bound
     * @param to       inclusive upper bound
     * @param pageable Spring pageable
     * @return page of matching rows
     */
    Page<InvoiceVerificationRecordEntity> findByVerificationTimeBetween(LocalDateTime from,
                                                                        LocalDateTime to,
                                                                        Pageable pageable);

    /**
     * Counts records carrying a given raw HNDEMP verification return code.
     *
     * @param code raw {@code invoCheckReturnCode} value
     * @return non-negative count
     */
    @Query("SELECT COUNT(r) FROM InvoiceVerificationRecordEntity r WHERE r.verificationResult = :code")
    long countByVerificationResult(@Param("code") String code);
}
