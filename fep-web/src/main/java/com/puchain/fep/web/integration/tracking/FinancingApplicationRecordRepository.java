package com.puchain.fep.web.integration.tracking;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link FinancingApplicationRecordEntity}
 * (§6.4.1 FR-DATA-DB-01).
 *
 * <p>Idempotent upsert keys on {@code applicationId} (the platform application
 * number); the future §5.9 report / ops layer reads by phase status. Mirrors
 * {@code ReconciliationRecordRepository}.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Repository
public interface FinancingApplicationRecordRepository
        extends JpaRepository<FinancingApplicationRecordEntity, String> {

    /**
     * Finds the unique record for a platform application number.
     * Used by the write path for idempotent upsert across phase updates.
     *
     * @param applicationId platform application number ({@code platApplyNo})
     * @return matching entity or empty
     */
    Optional<FinancingApplicationRecordEntity> findByApplicationId(String applicationId);

    /**
     * Pages records carrying a given raw HNDEMP phase code.
     *
     * @param approvalStatus raw {@code rzPhaseCode} value
     * @param pageable       Spring pageable
     * @return page of matching rows
     */
    Page<FinancingApplicationRecordEntity> findByApprovalStatus(String approvalStatus, Pageable pageable);

    /**
     * Counts records carrying a given raw HNDEMP phase code.
     *
     * @param approvalStatus raw {@code rzPhaseCode} value
     * @return non-negative count
     */
    @Query("SELECT COUNT(r) FROM FinancingApplicationRecordEntity r WHERE r.approvalStatus = :status")
    long countByApprovalStatus(@Param("status") String approvalStatus);
}
