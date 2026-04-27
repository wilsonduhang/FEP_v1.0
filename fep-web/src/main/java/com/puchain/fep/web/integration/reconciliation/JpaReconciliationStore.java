package com.puchain.fep.web.integration.reconciliation;

import com.puchain.fep.processor.reconciliation.ReconciliationRecord;
import com.puchain.fep.processor.reconciliation.ReconciliationStore;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * JPA-backed {@link ReconciliationStore} adapter for fep-web.
 *
 * <p>Registered under bean name {@code jpaReconciliationStore} with
 * {@link Primary}, satisfying the
 * {@code @ConditionalOnMissingBean(name = "jpaReconciliationStore")} guard on
 * {@code InMemoryReconciliationStore} — when fep-web is on the classpath the
 * JPA adapter fully replaces the in-memory implementation.</p>
 *
 * <p>Translates between the immutable domain record
 * {@link ReconciliationRecord} and the mutable JPA entity
 * {@link ReconciliationRecordEntity}.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component("jpaReconciliationStore")
@Primary
public class JpaReconciliationStore implements ReconciliationStore {

    private final ReconciliationRecordRepository repository;

    /**
     * Creates the adapter with the required Spring Data repository.
     *
     * @param repository JPA repository bean, non-null
     */
    public JpaReconciliationStore(final ReconciliationRecordRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    @Override
    @Transactional
    public ReconciliationRecord save(final ReconciliationRecord record) {
        Objects.requireNonNull(record, "record");
        final ReconciliationRecordEntity saved = repository.save(toEntity(record));
        return toRecord(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReconciliationRecord> findBySerialNoAndMessageType(final String serialNo,
                                                                       final String messageType) {
        Objects.requireNonNull(serialNo, "serialNo");
        Objects.requireNonNull(messageType, "messageType");
        return repository.findBySerialNoAndMessageType(serialNo, messageType)
                .map(JpaReconciliationStore::toRecord);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReconciliationRecord> findByDateAndStatus(final LocalDate date, final String status) {
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(status, "status");
        return repository.findByReconciliationDateAndReconciliationStatus(date, status)
                .stream()
                .map(JpaReconciliationStore::toRecord)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countByDate(final LocalDate date) {
        Objects.requireNonNull(date, "date");
        return repository.countByReconciliationDate(date);
    }

    private static ReconciliationRecordEntity toEntity(final ReconciliationRecord r) {
        final ReconciliationRecordEntity e = new ReconciliationRecordEntity();
        e.setReconciliationId(r.getReconciliationId());
        e.setReconciliationDate(r.getReconciliationDate());
        e.setMessageType(r.getMessageType());
        e.setSerialNo(r.getSerialNo());
        e.setPairedSerialNo(r.getPairedSerialNo());
        e.setTotalTransactionCount(r.getTotalTransactionCount());
        e.setTotalTransactionAmount(r.getTotalTransactionAmount());
        e.setActualCount(r.getActualCount());
        e.setReconciliationStatus(r.getStatus());
        e.setDiscrepancyCount(r.getDiscrepancyCount());
        e.setReconciliationTime(r.getReconciliationTime());
        e.setCreatedAt(r.getCreatedAt());
        e.setUpdatedAt(r.getUpdatedAt());
        return e;
    }

    private static ReconciliationRecord toRecord(final ReconciliationRecordEntity e) {
        return ReconciliationRecord.builder()
                .reconciliationId(e.getReconciliationId())
                .reconciliationDate(e.getReconciliationDate())
                .messageType(e.getMessageType())
                .serialNo(e.getSerialNo())
                .pairedSerialNo(e.getPairedSerialNo())
                .totalTransactionCount(e.getTotalTransactionCount())
                .totalTransactionAmount(e.getTotalTransactionAmount())
                .actualCount(e.getActualCount())
                .status(e.getReconciliationStatus())
                .discrepancyCount(e.getDiscrepancyCount())
                .reconciliationTime(e.getReconciliationTime())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
