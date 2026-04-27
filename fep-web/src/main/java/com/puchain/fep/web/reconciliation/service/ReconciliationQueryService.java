package com.puchain.fep.web.reconciliation.service;

import com.puchain.fep.processor.reconciliation.ReconciliationRecord;
import com.puchain.fep.web.integration.reconciliation.ReconciliationRecordEntity;
import com.puchain.fep.web.integration.reconciliation.ReconciliationRecordRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Read-side query service for {@link com.puchain.fep.processor.reconciliation.ReconciliationRecord}.
 *
 * <p>P2e Task 7 — wraps {@link ReconciliationRecordRepository} so that the
 * {@code ReconciliationController} stays compliant with ArchUnit rule
 * {@code controllers_must_not_directly_depend_on_repositories}. The Hexagonal
 * port {@link com.puchain.fep.processor.reconciliation.ReconciliationStore}
 * does not expose by-id / pageable queries (those concerns live entirely on the
 * read side and would pollute the write-side processor port), so this thin
 * service stitches the JPA Repository to the immutable domain record.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Service
public class ReconciliationQueryService {

    /** Sort direction by reconciliation execution time, descending. */
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "reconciliationTime");

    private final ReconciliationRecordRepository repository;

    /**
     * Constructs the query service.
     *
     * @param repository JPA repository for read-side access, non-null
     */
    public ReconciliationQueryService(final ReconciliationRecordRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    /**
     * Finds a record by its primary key.
     *
     * @param id reconciliation id (e.g. {@code RC_20260427_001})
     * @return optional record
     */
    @Transactional(readOnly = true)
    public Optional<ReconciliationRecord> findById(final String id) {
        return repository.findById(id).map(ReconciliationQueryService::toRecord);
    }

    /**
     * Finds the most recent record for the given message type and business date.
     *
     * @param messageType HNDEMP message type code
     * @param date        reconciliation business date
     * @return optional latest record
     */
    @Transactional(readOnly = true)
    public Optional<ReconciliationRecord> findLatestByMessageTypeAndDate(final String messageType,
                                                                         final LocalDate date) {
        Objects.requireNonNull(messageType, "messageType");
        Objects.requireNonNull(date, "date");
        final Page<ReconciliationRecordEntity> page =
                repository.findByMessageTypeAndReconciliationDate(
                        messageType, date, PageRequest.of(0, 1, DEFAULT_SORT));
        if (page.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(toRecord(page.getContent().get(0)));
    }

    /**
     * Pages records optionally filtered by date and/or message type.
     *
     * <p>{@code status} filtering is applied in-memory to the page content;
     * production should add a DB-side query when the dataset grows.</p>
     *
     * @param date        optional reconciliation date
     * @param messageType optional message type code
     * @param status      optional reconciliation status (in-memory filter)
     * @param pageNum     1-based page number
     * @param pageSize    page size
     * @return paged page (entity + total)
     */
    @Transactional(readOnly = true)
    public PagedResult search(final LocalDate date, final String messageType,
                              final String status, final int pageNum, final int pageSize) {
        final int safePage = Math.max(1, pageNum) - 1;
        final int safeSize = Math.max(1, pageSize);
        final Pageable pageable = PageRequest.of(safePage, safeSize, DEFAULT_SORT);

        final Page<ReconciliationRecordEntity> page;
        if (date != null) {
            if (messageType != null && !messageType.isBlank()) {
                page = repository.findByMessageTypeAndReconciliationDate(messageType, date, pageable);
            } else {
                page = repository.findByReconciliationDate(date, pageable);
            }
        } else {
            page = repository.findAll(pageable);
        }

        List<ReconciliationRecord> content = page.getContent().stream()
                .map(ReconciliationQueryService::toRecord)
                .toList();
        if (status != null && !status.isBlank()) {
            content = content.stream()
                    .filter(r -> status.equals(r.getStatus()))
                    .toList();
        }
        return new PagedResult(content, page.getTotalElements(), page.getTotalPages());
    }

    /**
     * Lifts a JPA entity into the immutable domain record.
     *
     * @param e entity, non-null
     * @return immutable record
     */
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

    /**
     * Immutable bundle of paged content + pagination metadata.
     *
     * @param content    page records
     * @param total      total elements across all pages
     * @param totalPages total pages
     */
    public record PagedResult(List<ReconciliationRecord> content, long total, int totalPages) {
    }
}
