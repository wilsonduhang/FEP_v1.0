package com.puchain.fep.web.integration.tracking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository behaviour for {@link BatchForwardRecordRepository}
 * (§6.4.1 FR-DATA-DB-01, PRD v1.3 §2020).
 *
 * <p>Uses {@code @SpringBootTest} (not {@code @DataJpaTest}) because the H2
 * {@code MODE=MySQL} DDL of {@code V41} needs the full Flyway + application
 * context, matching {@link InvoiceVerificationRecordRepositoryTest}.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@Transactional
@DisplayName("BatchForwardRecordRepository: persistence + finders + audit")
class BatchForwardRecordRepositoryTest {

    @Autowired
    private BatchForwardRecordRepository repository;

    @BeforeEach
    void cleanUp() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("save then findBySerialNo returns the persisted row with mapped fields")
    void save_thenFindBySerialNo_returnsRow() {
        repository.save(sample("BF-1", "SN-1", "COMPLETED", 10, 10));

        final Optional<BatchForwardRecordEntity> found = repository.findBySerialNo("SN-1");

        assertThat(found).isPresent();
        assertThat(found.get().getBatchForwardId()).isEqualTo("BF-1");
        assertThat(found.get().getBatchType()).isEqualTo("3009");
        assertThat(found.get().getTotalRecordCount()).isEqualTo(10);
        assertThat(found.get().getSuccessRecordCount()).isEqualTo(10);
        assertThat(found.get().getBatchStatus()).isEqualTo("COMPLETED");
        assertThat(found.get().getErrorLogPath()).isNull();
    }

    @Test
    @DisplayName("@PrePersist fills created_at / updated_at when left null")
    void prePersist_fillsAuditTimestamps() {
        final BatchForwardRecordEntity saved = repository.save(sample("BF-2", "SN-2", "COMPLETED", 5, 5));

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("findByProcessStartTimeBetween paginates within the time window")
    void findByProcessStartTimeBetween_paginates() {
        final LocalDateTime base = LocalDateTime.of(2026, 6, 16, 10, 0);
        for (int i = 0; i < 5; i++) {
            final BatchForwardRecordEntity e = sample("BF-3" + i, "SN-3" + i, "COMPLETED", 3, 3);
            e.setProcessStartTime(base.plusMinutes(i));
            repository.save(e);
        }

        assertThat(repository.findByProcessStartTimeBetween(
                base.minusHours(1), base.plusHours(1), PageRequest.of(0, 3))
                .getTotalElements()).isEqualTo(5L);
    }

    @Test
    @DisplayName("countByBatchStatus counts rows for a given raw status")
    void countByBatchStatus_countsByRawStatus() {
        repository.save(sample("BF-40", "SN-40", "COMPLETED", 1, 1));
        repository.save(sample("BF-41", "SN-41", "COMPLETED", 1, 1));
        repository.save(sample("BF-42", "SN-42", "FAILED", 2, 1));

        assertThat(repository.countByBatchStatus("COMPLETED")).isEqualTo(2L);
        assertThat(repository.countByBatchStatus("FAILED")).isEqualTo(1L);
    }

    private static BatchForwardRecordEntity sample(final String id,
                                                   final String serialNo,
                                                   final String status,
                                                   final int total,
                                                   final int success) {
        final BatchForwardRecordEntity e = new BatchForwardRecordEntity();
        e.setBatchForwardId(id);
        e.setBatchType("3009");
        e.setTotalRecordCount(total);
        e.setSuccessRecordCount(success);
        e.setProcessStartTime(LocalDateTime.of(2026, 6, 16, 10, 0));
        e.setProcessEndTime(LocalDateTime.of(2026, 6, 16, 10, 1));
        e.setBatchStatus(status);
        e.setErrorLogPath(null);
        e.setSerialNo(serialNo);
        // createdAt / updatedAt left null to exercise @PrePersist fallback
        return e;
    }
}
