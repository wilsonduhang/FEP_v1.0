package com.puchain.fep.web.integration.reconciliation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository behaviour for {@link ReconciliationRecordRepository}.
 *
 * <p>Uses {@code @SpringBootTest} (not {@code @DataJpaTest}) because the H2
 * {@code MODE=MySQL} DDL needs the full Flyway + application context, matching
 * the pattern set by {@code SubSubmissionRecordRepositoryTest}.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@Transactional
@DisplayName("ReconciliationRecordRepository: CRUD + finder")
class ReconciliationRecordRepositoryTest {

    @Autowired
    private ReconciliationRecordRepository repository;

    @BeforeEach
    void cleanUp() {
        repository.deleteAll();
    }

    @Test
    void save_andFindBySerialNoAndMessageType() {
        final ReconciliationRecordEntity entity = sampleEntity("R-RP1", "SN-RP1", "3107",
                LocalDate.of(2026, 4, 27), "PENDING");
        repository.save(entity);

        final Optional<ReconciliationRecordEntity> found =
                repository.findBySerialNoAndMessageType("SN-RP1", "3107");

        assertThat(found)
                .map(ReconciliationRecordEntity::getReconciliationId)
                .contains("R-RP1");
    }

    @Test
    void findByReconciliationDateAndReconciliationStatus_shouldFilterCorrectly() {
        final LocalDate target = LocalDate.of(2026, 4, 27);
        repository.save(sampleEntity("R-RP10", "SN-RP10", "3107", target, "COMPLETED"));
        repository.save(sampleEntity("R-RP11", "SN-RP11", "3108", target, "COMPLETED"));
        repository.save(sampleEntity("R-RP12", "SN-RP12", "3107", target, "PENDING"));

        final List<ReconciliationRecordEntity> completed =
                repository.findByReconciliationDateAndReconciliationStatus(target, "COMPLETED");

        assertThat(completed).hasSize(2)
                .extracting(ReconciliationRecordEntity::getReconciliationId)
                .containsExactlyInAnyOrder("R-RP10", "R-RP11");
    }

    @Test
    void findByReconciliationDate_shouldPaginate() {
        final LocalDate target = LocalDate.of(2026, 4, 27);
        for (int i = 0; i < 5; i++) {
            repository.save(sampleEntity("R-RP2" + i, "SN-RP2" + i, "3107", target, "PENDING"));
        }

        assertThat(repository.findByReconciliationDate(target, PageRequest.of(0, 3))
                .getTotalElements()).isEqualTo(5L);
    }

    @Test
    void countByReconciliationDate_shouldReturnRowCount() {
        final LocalDate target = LocalDate.of(2026, 4, 27);
        repository.save(sampleEntity("R-RP30", "SN-RP30", "3107", target, "PENDING"));
        repository.save(sampleEntity("R-RP31", "SN-RP31", "3108", target, "PENDING"));
        repository.save(sampleEntity("R-RP32", "SN-RP32", "3107", target.plusDays(1), "PENDING"));

        assertThat(repository.countByReconciliationDate(target)).isEqualTo(2L);
        assertThat(repository.countByReconciliationDate(target.plusDays(1))).isEqualTo(1L);
    }

    private static ReconciliationRecordEntity sampleEntity(final String id,
                                                           final String serialNo,
                                                           final String messageType,
                                                           final LocalDate date,
                                                           final String status) {
        final ReconciliationRecordEntity e = new ReconciliationRecordEntity();
        e.setReconciliationId(id);
        e.setReconciliationDate(date);
        e.setMessageType(messageType);
        e.setSerialNo(serialNo);
        e.setTotalTransactionCount(0);
        e.setTotalTransactionAmount(BigDecimal.ZERO);
        e.setActualCount(0);
        e.setReconciliationStatus(status);
        e.setDiscrepancyCount(0);
        e.setReconciliationTime(LocalDateTime.of(date, java.time.LocalTime.NOON));
        // createdAt / updatedAt left null intentionally to exercise @PrePersist fallback
        return e;
    }
}
