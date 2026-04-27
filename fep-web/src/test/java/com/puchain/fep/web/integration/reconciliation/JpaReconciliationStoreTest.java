package com.puchain.fep.web.integration.reconciliation;

import com.puchain.fep.processor.reconciliation.ReconciliationRecord;
import com.puchain.fep.processor.reconciliation.ReconciliationStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end test for {@link JpaReconciliationStore} adapter.
 *
 * <p>Verifies POJO ↔ Entity round-trip, repository delegation, and that the
 * adapter is the {@code @Primary} {@link ReconciliationStore} bean injected by
 * Spring (in-memory implementation gives way via
 * {@code @ConditionalOnMissingBean(name = "jpaReconciliationStore")}).</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@Transactional
@DisplayName("JpaReconciliationStore: adapter round-trip")
class JpaReconciliationStoreTest {

    @Autowired
    private ReconciliationStore store;

    @Autowired
    private ReconciliationRecordRepository repository;

    @BeforeEach
    void cleanUp() {
        repository.deleteAll();
    }

    @Test
    void primaryAdapter_shouldBeJpaImplementation() {
        // 红线核查：Spring 必须注入 JpaReconciliationStore 而非 InMemory 默认实现
        assertThat(store).isInstanceOf(JpaReconciliationStore.class);
    }

    @Test
    void save_shouldRoundTripAllFields() {
        final LocalDate date = LocalDate.of(2026, 4, 27);
        final LocalDateTime now = LocalDateTime.of(date, java.time.LocalTime.NOON);
        final ReconciliationRecord record = ReconciliationRecord.builder()
                .reconciliationId("R-AD1")
                .reconciliationDate(date)
                .messageType("3107")
                .serialNo("SN-AD1")
                .pairedSerialNo("PSN-AD1")
                .totalTransactionCount(100)
                .totalTransactionAmount(new BigDecimal("12345.6789"))
                .actualCount(99)
                .status("DISCREPANCY")
                .discrepancyCount(1)
                .reconciliationTime(now)
                .createdAt(now)
                .updatedAt(now)
                .build();

        final ReconciliationRecord saved = store.save(record);

        assertThat(saved.getReconciliationId()).isEqualTo("R-AD1");
        assertThat(saved.getPairedSerialNo()).isEqualTo("PSN-AD1");
        assertThat(saved.getTotalTransactionAmount()).isEqualByComparingTo(new BigDecimal("12345.6789"));
        assertThat(saved.getStatus()).isEqualTo("DISCREPANCY");
        assertThat(saved.getDiscrepancyCount()).isEqualTo(1);
    }

    @Test
    void findersAndCounters_shouldDelegateToRepository() {
        final LocalDate target = LocalDate.of(2026, 4, 27);
        final LocalDateTime now = LocalDateTime.of(target, java.time.LocalTime.NOON);

        store.save(ReconciliationRecord.builder()
                .reconciliationId("R-AD2")
                .reconciliationDate(target)
                .messageType("3107")
                .serialNo("SN-AD2")
                .totalTransactionCount(0)
                .totalTransactionAmount(BigDecimal.ZERO)
                .actualCount(0)
                .status("PENDING")
                .discrepancyCount(0)
                .reconciliationTime(now)
                .createdAt(now)
                .updatedAt(now)
                .build());

        final Optional<ReconciliationRecord> byKey =
                store.findBySerialNoAndMessageType("SN-AD2", "3107");
        assertThat(byKey).map(ReconciliationRecord::getReconciliationId).contains("R-AD2");

        assertThat(store.findByDateAndStatus(target, "PENDING")).hasSize(1);
        assertThat(store.countByDate(target)).isEqualTo(1L);
    }
}
