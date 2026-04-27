package com.puchain.fep.web.integration.reconciliation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link ClearingInstructionRecordEntity}.
 *
 * <p>Composite primary key {@link ClearingInstructionRecordEntity.PK} drives the
 * {@code JpaRepository} type parameter.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Repository
public interface ClearingInstructionRecordRepository
        extends JpaRepository<ClearingInstructionRecordEntity, ClearingInstructionRecordEntity.PK> {

    /**
     * Finds the unique record matching the composite primary key.
     *
     * @param instructionId clearing instruction id
     * @param qsSerialNo    clearing serial number
     * @return matching entity or empty
     */
    Optional<ClearingInstructionRecordEntity> findByInstructionIdAndQsSerialNo(String instructionId, String qsSerialNo);

    /**
     * Lists clearing instructions linked to a given inbound message.
     *
     * @param messageId message process record id
     * @return matching rows, possibly empty
     */
    List<ClearingInstructionRecordEntity> findByMessageId(String messageId);

    /**
     * Lists clearing instructions in a given status. Used by retry / monitoring scans.
     *
     * @param instructionStatus instruction status string
     * @return matching rows, possibly empty
     */
    List<ClearingInstructionRecordEntity> findByInstructionStatus(String instructionStatus);
}
