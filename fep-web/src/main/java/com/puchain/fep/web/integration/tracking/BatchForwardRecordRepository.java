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
 * Spring Data JPA repository for {@link BatchForwardRecordEntity}
 * (§6.4.1 FR-DATA-DB-01, PRD v1.3 §2020 非实时业务转发记录表).
 *
 * <p>Exposes the minimum query surface required by
 * {@code BatchForwardTrackingService} (idempotent upsert by serial number) and
 * the future §5.9 report / ops layer (time-window listing + raw-status count).
 * Mirrors {@link InvoiceVerificationRecordRepository}.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Repository
public interface BatchForwardRecordRepository
        extends JpaRepository<BatchForwardRecordEntity, String> {

    /**
     * Finds the unique record for a business serial number.
     * Backed by unique constraint {@code uq_batch_forward_serial}; used by the
     * write path for idempotent upsert.
     *
     * @param serialNo business serial number
     * @return matching entity or empty
     */
    Optional<BatchForwardRecordEntity> findBySerialNo(String serialNo);

    /**
     * Pages records whose process start time falls within {@code [from, to]}.
     *
     * @param from     inclusive lower bound
     * @param to       inclusive upper bound
     * @param pageable Spring pageable
     * @return page of matching rows
     */
    Page<BatchForwardRecordEntity> findByProcessStartTimeBetween(LocalDateTime from,
                                                                 LocalDateTime to,
                                                                 Pageable pageable);

    /**
     * Counts records carrying a given raw batch status (state-machine state name).
     *
     * @param status raw batch status value
     * @return non-negative count
     */
    @Query("SELECT COUNT(r) FROM BatchForwardRecordEntity r WHERE r.batchStatus = :status")
    long countByBatchStatus(@Param("status") String status);
}
