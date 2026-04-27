package com.puchain.fep.processor.reconciliation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CRUD + finder behaviour for {@link InMemoryReconciliationStore}.
 *
 * @author FEP Team
 * @since 1.0.0
 */
@DisplayName("InMemoryReconciliationStore: CRUD + finder")
class InMemoryReconciliationStoreTest {

    private InMemoryReconciliationStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryReconciliationStore();
    }

    @Test
    void save_shouldPersistAndReturnSameRecord() {
        final ReconciliationRecord record = sampleRecord("R1", "SN1", "3107",
                LocalDate.of(2026, 4, 27), "PENDING");

        final ReconciliationRecord saved = store.save(record);

        assertThat(saved).isSameAs(record);
        assertThat(store.findBySerialNoAndMessageType("SN1", "3107"))
                .containsSame(record);
    }

    @Test
    void findBySerialNoAndMessageType_shouldReturnEmpty_whenNoMatch() {
        store.save(sampleRecord("R1", "SN1", "3107", LocalDate.of(2026, 4, 27), "PENDING"));

        final Optional<ReconciliationRecord> result =
                store.findBySerialNoAndMessageType("MISSING", "3107");

        assertThat(result).isEmpty();
    }

    @Test
    void findByDateAndStatus_shouldFilterByBothColumns() {
        final LocalDate target = LocalDate.of(2026, 4, 27);
        store.save(sampleRecord("R1", "SN1", "3107", target, "COMPLETED"));
        store.save(sampleRecord("R2", "SN2", "3108", target, "PENDING"));
        store.save(sampleRecord("R3", "SN3", "3107", target, "COMPLETED"));
        store.save(sampleRecord("R4", "SN4", "3107", target.plusDays(1), "COMPLETED"));

        final List<ReconciliationRecord> results = store.findByDateAndStatus(target, "COMPLETED");

        assertThat(results).hasSize(2)
                .extracting(ReconciliationRecord::getReconciliationId)
                .containsExactlyInAnyOrder("R1", "R3");
    }

    @Test
    void countByDate_shouldReturnTotalAcrossStatuses() {
        final LocalDate target = LocalDate.of(2026, 4, 27);
        store.save(sampleRecord("R1", "SN1", "3107", target, "PENDING"));
        store.save(sampleRecord("R2", "SN2", "3108", target, "COMPLETED"));
        store.save(sampleRecord("R3", "SN3", "3107", target.plusDays(1), "PENDING"));

        assertThat(store.countByDate(target)).isEqualTo(2L);
        assertThat(store.countByDate(target.plusDays(1))).isEqualTo(1L);
        assertThat(store.countByDate(target.plusDays(2))).isZero();
    }

    @Test
    void save_shouldOverwriteByReconciliationId() {
        final LocalDate target = LocalDate.of(2026, 4, 27);
        final ReconciliationRecord first = sampleRecord("R1", "SN1", "3107", target, "PENDING");
        store.save(first);

        final ReconciliationRecord updated = ReconciliationRecord.builder()
                .from(first)
                .status("COMPLETED")
                .updatedAt(LocalDateTime.now())
                .build();
        store.save(updated);

        assertThat(store.findBySerialNoAndMessageType("SN1", "3107"))
                .map(ReconciliationRecord::getStatus)
                .contains("COMPLETED");
    }

    private static ReconciliationRecord sampleRecord(final String id,
                                                     final String serialNo,
                                                     final String messageType,
                                                     final LocalDate date,
                                                     final String status) {
        final LocalDateTime now = LocalDateTime.of(date, java.time.LocalTime.NOON);
        return ReconciliationRecord.builder()
                .reconciliationId(id)
                .reconciliationDate(date)
                .messageType(messageType)
                .serialNo(serialNo)
                .totalTransactionCount(0)
                .totalTransactionAmount(BigDecimal.ZERO)
                .actualCount(0)
                .status(status)
                .discrepancyCount(0)
                .reconciliationTime(now)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
