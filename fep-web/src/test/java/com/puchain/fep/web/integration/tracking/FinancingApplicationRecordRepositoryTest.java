package com.puchain.fep.web.integration.tracking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository behaviour for {@link FinancingApplicationRecordRepository}
 * (§6.4.1 FR-DATA-DB-01, PRD v1.3 §1945). Uses {@code @SpringBootTest} because
 * the H2 {@code MODE=MySQL} DDL of {@code V39} needs the full Flyway context,
 * matching {@code ReconciliationRecordRepositoryTest}.
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@Transactional
@DisplayName("FinancingApplicationRecordRepository: persistence + finders + audit")
class FinancingApplicationRecordRepositoryTest {

    @Autowired
    private FinancingApplicationRecordRepository repository;

    @BeforeEach
    void cleanUp() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("save then findByApplicationId returns the persisted row with mapped fields")
    void save_thenFindByApplicationId_returnsRow() {
        repository.save(sample("APP-1", "SN-1", "1", new BigDecimal("50000.00")));

        final Optional<FinancingApplicationRecordEntity> found =
                repository.findByApplicationId("APP-1");

        assertThat(found).isPresent();
        assertThat(found.get().getSerialNo()).isEqualTo("SN-1");
        assertThat(found.get().getApprovalStatus()).isEqualTo("1");
        assertThat(found.get().getApplicationAmount()).isEqualByComparingTo("50000.00");
    }

    @Test
    @DisplayName("@PrePersist fills created_at / updated_at when left null")
    void prePersist_fillsAuditTimestamps() {
        final FinancingApplicationRecordEntity saved =
                repository.save(sample("APP-2", "SN-2", "1", BigDecimal.ONE));

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("findByApprovalStatus paginates by raw phase code")
    void findByApprovalStatus_paginates() {
        for (int i = 0; i < 5; i++) {
            repository.save(sample("APP-3" + i, "SN-3" + i, "1", BigDecimal.TEN));
        }
        repository.save(sample("APP-99", "SN-99", "2", BigDecimal.TEN));

        assertThat(repository.findByApprovalStatus("1", PageRequest.of(0, 3))
                .getTotalElements()).isEqualTo(5L);
    }

    @Test
    @DisplayName("countByApprovalStatus counts rows for a given raw phase code")
    void countByApprovalStatus_countsByRawCode() {
        repository.save(sample("APP-40", "SN-40", "1", BigDecimal.TEN));
        repository.save(sample("APP-41", "SN-41", "1", BigDecimal.TEN));
        repository.save(sample("APP-42", "SN-42", "2", BigDecimal.TEN));

        assertThat(repository.countByApprovalStatus("1")).isEqualTo(2L);
        assertThat(repository.countByApprovalStatus("2")).isEqualTo(1L);
    }

    private static FinancingApplicationRecordEntity sample(final String applicationId,
                                                           final String serialNo,
                                                           final String status,
                                                           final BigDecimal amount) {
        final FinancingApplicationRecordEntity e = new FinancingApplicationRecordEntity();
        e.setApplicationId(applicationId);
        e.setCoreEnterpriseName("核心企业A");
        e.setRzpzNo("RZPZ-001");
        e.setApplicationAmount(amount);
        e.setApprovalAmount(amount);
        e.setApplicationTime(LocalDateTime.of(2026, 6, 14, 10, 0));
        e.setApprovalStatus(status);
        e.setResultNoticeTime(LocalDateTime.of(2026, 6, 14, 11, 0));
        e.setSerialNo(serialNo);
        // createdAt / updatedAt left null to exercise @PrePersist fallback
        return e;
    }
}
