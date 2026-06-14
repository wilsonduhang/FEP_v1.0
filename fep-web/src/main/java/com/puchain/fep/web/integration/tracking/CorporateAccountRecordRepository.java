package com.puchain.fep.web.integration.tracking;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link CorporateAccountRecordEntity}
 * (§6.4.1 FR-DATA-DB-01).
 *
 * <p>Idempotent upsert keys on {@code enterpriseId} (the corporate USCI); the
 * future §5.9 report / ops layer reads by account status. Mirrors
 * {@code ReconciliationRecordRepository}.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Repository
public interface CorporateAccountRecordRepository
        extends JpaRepository<CorporateAccountRecordEntity, String> {

    /**
     * Finds the unique record for a corporate USCI.
     * Used by the write path for idempotent upsert across re-verifications.
     *
     * @param enterpriseId corporate USCI ({@code qyAccCode})
     * @return matching entity or empty
     */
    Optional<CorporateAccountRecordEntity> findByEnterpriseId(String enterpriseId);

    /**
     * Pages records carrying a given raw HNDEMP account return code.
     *
     * @param accountStatus raw {@code accReturnCode} value
     * @param pageable      Spring pageable
     * @return page of matching rows
     */
    Page<CorporateAccountRecordEntity> findByAccountStatus(String accountStatus, Pageable pageable);

    /**
     * Counts records carrying a given raw HNDEMP account return code.
     *
     * @param accountStatus raw {@code accReturnCode} value
     * @return non-negative count
     */
    @Query("SELECT COUNT(r) FROM CorporateAccountRecordEntity r WHERE r.accountStatus = :status")
    long countByAccountStatus(@Param("status") String accountStatus);
}
