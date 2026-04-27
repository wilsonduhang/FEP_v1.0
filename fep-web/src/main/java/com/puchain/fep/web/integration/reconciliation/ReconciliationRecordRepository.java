package com.puchain.fep.web.integration.reconciliation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link ReconciliationRecordEntity}.
 *
 * <p>Exposes the minimum query surface required by {@code JpaReconciliationStore}
 * and the future P2e Web layer (Task 6).</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Repository
public interface ReconciliationRecordRepository
        extends JpaRepository<ReconciliationRecordEntity, String> {

    /**
     * Finds the unique record matching ({@code serialNo}, {@code messageType}).
     * Backed by unique constraint {@code uq_recon_serial_message}.
     *
     * @param serialNo    business serial number
     * @param messageType HNDEMP message type code
     * @return matching entity or empty
     */
    Optional<ReconciliationRecordEntity> findBySerialNoAndMessageType(String serialNo, String messageType);

    /**
     * Lists records for a given reconciliation date and status.
     *
     * @param date   reconciliation date
     * @param status reconciliation status string
     * @return matching rows, possibly empty
     */
    List<ReconciliationRecordEntity> findByReconciliationDateAndReconciliationStatus(LocalDate date, String status);

    /**
     * Pages all records for a given reconciliation date.
     *
     * @param date     reconciliation date
     * @param pageable Spring pageable
     * @return page of matching rows
     */
    Page<ReconciliationRecordEntity> findByReconciliationDate(LocalDate date, Pageable pageable);

    /**
     * Pages records for a given message type and reconciliation date.
     *
     * @param messageType HNDEMP message type code
     * @param date        reconciliation date
     * @param pageable    Spring pageable
     * @return page of matching rows
     */
    Page<ReconciliationRecordEntity> findByMessageTypeAndReconciliationDate(String messageType,
                                                                             LocalDate date,
                                                                             Pageable pageable);

    /**
     * Counts records for a given reconciliation date.
     *
     * @param date reconciliation date
     * @return non-negative count
     */
    @Query("SELECT COUNT(r) FROM ReconciliationRecordEntity r WHERE r.reconciliationDate = :date")
    long countByReconciliationDate(@Param("date") LocalDate date);
}
