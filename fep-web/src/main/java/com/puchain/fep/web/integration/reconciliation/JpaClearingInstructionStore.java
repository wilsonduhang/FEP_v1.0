package com.puchain.fep.web.integration.reconciliation;

import com.puchain.fep.processor.reconciliation.ClearingInstructionRecord;
import com.puchain.fep.processor.reconciliation.ClearingInstructionStore;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * JPA-backed {@link ClearingInstructionStore} adapter for fep-web.
 *
 * <p>Registered under bean name {@code jpaClearingInstructionStore} with
 * {@link Primary}, satisfying the
 * {@code @ConditionalOnMissingBean(name = "jpaClearingInstructionStore")} guard on
 * {@code InMemoryClearingInstructionStore}.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component("jpaClearingInstructionStore")
@Primary
public class JpaClearingInstructionStore implements ClearingInstructionStore {

    private final ClearingInstructionRecordRepository repository;

    /**
     * Creates the adapter with the required Spring Data repository.
     *
     * @param repository JPA repository bean, non-null
     */
    public JpaClearingInstructionStore(final ClearingInstructionRecordRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    @Override
    @Transactional
    public ClearingInstructionRecord save(final ClearingInstructionRecord record) {
        Objects.requireNonNull(record, "record");
        final ClearingInstructionRecordEntity saved = repository.save(toEntity(record));
        return toRecord(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ClearingInstructionRecord> findByInstructionIdAndQsSerialNo(final String instructionId,
                                                                                 final String qsSerialNo) {
        Objects.requireNonNull(instructionId, "instructionId");
        Objects.requireNonNull(qsSerialNo, "qsSerialNo");
        return repository.findByInstructionIdAndQsSerialNo(instructionId, qsSerialNo)
                .map(JpaClearingInstructionStore::toRecord);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClearingInstructionRecord> findByMessageId(final String messageId) {
        Objects.requireNonNull(messageId, "messageId");
        return repository.findByMessageId(messageId)
                .stream()
                .map(JpaClearingInstructionStore::toRecord)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClearingInstructionRecord> findByStatus(final String status) {
        Objects.requireNonNull(status, "status");
        return repository.findByInstructionStatus(status)
                .stream()
                .map(JpaClearingInstructionStore::toRecord)
                .toList();
    }

    private static ClearingInstructionRecordEntity toEntity(final ClearingInstructionRecord r) {
        final ClearingInstructionRecordEntity e = new ClearingInstructionRecordEntity();
        e.setInstructionId(r.getInstructionId());
        e.setQsSerialNo(r.getQsSerialNo());
        e.setInstructionType(r.getInstructionType());
        e.setSettlementAmount(r.getSettlementAmount());
        e.setPayerAccount(r.getPayerAccount());
        e.setPayeeAccount(r.getPayeeAccount());
        e.setInstructionStatus(r.getInstructionStatus());
        e.setExecutionTime(r.getExecutionTime());
        e.setFailureCause(r.getFailureCause());
        e.setMessageId(r.getMessageId());
        e.setCreatedAt(r.getCreatedAt());
        e.setUpdatedAt(r.getUpdatedAt());
        return e;
    }

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
