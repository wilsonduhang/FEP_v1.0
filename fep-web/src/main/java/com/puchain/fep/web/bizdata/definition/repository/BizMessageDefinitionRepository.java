package com.puchain.fep.web.bizdata.definition.repository;

import com.puchain.fep.common.domain.EnableDisableStatus;
import com.puchain.fep.web.bizdata.definition.domain.BizMessageDefinition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for {@link BizMessageDefinition}.
 *
 * <p>Provides CRUD, uniqueness checks, and keyword search for message definitions.
 * See PRD v1.3 section 5.3.1 + section 5.3.2.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Repository
public interface BizMessageDefinitionRepository
        extends JpaRepository<BizMessageDefinition, String> {

    /**
     * Check if a message code already exists.
     *
     * @param messageCode message code
     * @return true if exists
     */
    boolean existsByMessageCode(String messageCode);

    /**
     * Check if a message code already exists, excluding a specific definition.
     *
     * @param messageCode  message code
     * @param definitionId definition ID to exclude
     * @return true if exists
     */
    @Query("SELECT COUNT(d) > 0 FROM BizMessageDefinition d "
            + "WHERE d.messageCode = :code AND d.definitionId <> :id")
    boolean existsByMessageCodeAndDefinitionIdNot(
            @Param("code") String messageCode,
            @Param("id") String definitionId);

    /**
     * Search definitions by keyword (matches messageCode or messageName).
     *
     * @param keyword  keyword (may be null for all)
     * @param pageable pagination params
     * @return paginated results
     */
    @Query("SELECT d FROM BizMessageDefinition d "
            + "WHERE (:keyword IS NULL "
            + "OR d.messageCode LIKE %:keyword% "
            + "OR d.messageName LIKE %:keyword%)")
    Page<BizMessageDefinition> search(@Param("keyword") String keyword,
                                       Pageable pageable);

    /**
     * Find definitions by status.
     *
     * @param status definition status
     * @return list of matching definitions
     */
    List<BizMessageDefinition> findByDefinitionStatus(EnableDisableStatus status);
}
