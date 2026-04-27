package com.puchain.fep.web.reconciliation.service;

import com.puchain.fep.processor.reconciliation.ClearingInstructionRecord;
import com.puchain.fep.web.integration.reconciliation.ClearingInstructionRecordEntity;
import com.puchain.fep.web.integration.reconciliation.ClearingInstructionRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Read-side query service for {@link ClearingInstructionRecord}.
 *
 * <p>P2e Task 7 — wraps {@link ClearingInstructionRecordRepository} so that the
 * {@code SettlementInstructionController} stays compliant with ArchUnit rule
 * {@code controllers_must_not_directly_depend_on_repositories}. The Hexagonal
 * port {@link com.puchain.fep.processor.reconciliation.ClearingInstructionStore}
 * does not expose by-instructionId queries (the by-platPayNo lookup is a
 * read-only Web concern), so this thin service stitches the JPA Repository
 * to the immutable domain record.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Service
public class SettlementInstructionQueryService {

    private final ClearingInstructionRecordRepository repository;

    /**
     * Constructs the query service.
     *
     * @param repository JPA repository for read-side access, non-null
     */
    public SettlementInstructionQueryService(final ClearingInstructionRecordRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    /**
     * Lists all instruction rows sharing the given {@code platPayNo} (= instructionId).
     * Rows are sorted by {@code qsSerialNo} ascending.
     *
     * @param platPayNo platform settlement instruction number
     * @return list of records, possibly empty
     */
    @Transactional(readOnly = true)
    public List<ClearingInstructionRecord> findByPlatPayNo(final String platPayNo) {
        Objects.requireNonNull(platPayNo, "platPayNo");
        return repository.findByInstructionIdOrderByQsSerialNoAsc(platPayNo).stream()
                .map(SettlementInstructionQueryService::toRecord)
                .toList();
    }

    /**
     * Lifts a JPA entity into the immutable domain record.
     *
     * @param e entity, non-null
     * @return immutable record
     */
    private static ClearingInstructionRecord toRecord(final ClearingInstructionRecordEntity e) {
        return ClearingInstructionRecord.builder()
                .instructionId(e.getInstructionId())
                .qsSerialNo(e.getQsSerialNo())
                .instructionType(e.getInstructionType())
                .settlementAmount(e.getSettlementAmount())
                .payerAccount(e.getPayerAccount())
                .payeeAccount(e.getPayeeAccount())
                .instructionStatus(e.getInstructionStatus())
                .executionTime(e.getExecutionTime())
                .failureCause(e.getFailureCause())
                .messageId(e.getMessageId())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
