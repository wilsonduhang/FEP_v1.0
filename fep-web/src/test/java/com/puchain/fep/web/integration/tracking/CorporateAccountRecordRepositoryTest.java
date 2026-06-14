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
 * Repository behaviour for {@link CorporateAccountRecordRepository}
 * (§6.4.1 FR-DATA-DB-01, PRD v1.3 §1958). Uses {@code @SpringBootTest} because
 * the H2 {@code MODE=MySQL} DDL of {@code V40} needs the full Flyway context.
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@Transactional
@DisplayName("CorporateAccountRecordRepository: persistence + finders + audit")
class CorporateAccountRecordRepositoryTest {

    @Autowired
    private CorporateAccountRecordRepository repository;

    @BeforeEach
    void cleanUp() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("save then findByEnterpriseId returns the persisted row with mapped fields")
    void save_thenFindByEnterpriseId_returnsRow() {
        repository.save(sample("USCI-1", "SN-1", "0"));

        final Optional<CorporateAccountRecordEntity> found =
                repository.findByEnterpriseId("USCI-1");

        assertThat(found).isPresent();
        assertThat(found.get().getAccountName()).isEqualTo("某企业账户");
        assertThat(found.get().getAccountStatus()).isEqualTo("0");
        assertThat(found.get().getSerialNo()).isEqualTo("SN-1");
    }

    @Test
    @DisplayName("@PrePersist fills created_at / updated_at when left null")
    void prePersist_fillsAuditTimestamps() {
        final CorporateAccountRecordEntity saved = repository.save(sample("USCI-2", "SN-2", "0"));

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("findByAccountStatus paginates by raw return code")
    void findByAccountStatus_paginates() {
        for (int i = 0; i < 4; i++) {
            repository.save(sample("USCI-3" + i, "SN-3" + i, "0"));
        }
        repository.save(sample("USCI-99", "SN-99", "1"));

        assertThat(repository.findByAccountStatus("0", PageRequest.of(0, 2))
                .getTotalElements()).isEqualTo(4L);
    }

    @Test
    @DisplayName("countByAccountStatus counts rows for a given raw return code")
    void countByAccountStatus_countsByRawCode() {
        repository.save(sample("USCI-40", "SN-40", "0"));
        repository.save(sample("USCI-41", "SN-41", "0"));
        repository.save(sample("USCI-42", "SN-42", "1"));

        assertThat(repository.countByAccountStatus("0")).isEqualTo(2L);
        assertThat(repository.countByAccountStatus("1")).isEqualTo(1L);
    }

    private static CorporateAccountRecordEntity sample(final String enterpriseId,
                                                       final String serialNo,
                                                       final String status) {
        final CorporateAccountRecordEntity e = new CorporateAccountRecordEntity();
        e.setEnterpriseId(enterpriseId);
        e.setAccountName("某企业账户");
        e.setAccountStatus(status);
        e.setStatusMemo("memo");
        e.setLastVerificationTime(LocalDateTime.of(2026, 6, 14, 10, 0));
        e.setSerialNo(serialNo);
        // createdAt / updatedAt left null to exercise @PrePersist fallback
        return e;
    }
}
