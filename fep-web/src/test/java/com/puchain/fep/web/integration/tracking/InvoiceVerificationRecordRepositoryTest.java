package com.puchain.fep.web.integration.tracking;

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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository behaviour for {@link InvoiceVerificationRecordRepository}
 * (§6.4.1 FR-DATA-DB-01, PRD v1.3 §1970).
 *
 * <p>Uses {@code @SpringBootTest} (not {@code @DataJpaTest}) because the H2
 * {@code MODE=MySQL} DDL of {@code V38} needs the full Flyway + application
 * context, matching the pattern set by
 * {@code ReconciliationRecordRepositoryTest}.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@Transactional
@DisplayName("InvoiceVerificationRecordRepository: persistence + finders + audit")
class InvoiceVerificationRecordRepositoryTest {

    @Autowired
    private InvoiceVerificationRecordRepository repository;

    @BeforeEach
    void cleanUp() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("save then findBySerialNo returns the persisted row with mapped fields")
    void save_thenFindBySerialNo_returnsRow() {
        repository.save(sample("IV-1", "SN-1", "0", new BigDecimal("1234.56")));

        final Optional<InvoiceVerificationRecordEntity> found =
                repository.findBySerialNo("SN-1");

        assertThat(found).isPresent();
        assertThat(found.get().getInvoiceId()).isEqualTo("IV-1");
        assertThat(found.get().getInvoiceCode()).isEqualTo("123456789012");
        assertThat(found.get().getInvoiceAmount()).isEqualByComparingTo("1234.56");
        assertThat(found.get().getVerificationResult()).isEqualTo("0");
    }

    @Test
    @DisplayName("@PrePersist fills created_at / updated_at when left null")
    void prePersist_fillsAuditTimestamps() {
        final InvoiceVerificationRecordEntity saved =
                repository.save(sample("IV-2", "SN-2", "0", BigDecimal.ONE));

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("findByVerificationTimeBetween paginates within the time window")
    void findByVerificationTimeBetween_paginates() {
        final LocalDateTime base = LocalDateTime.of(2026, 6, 14, 10, 0);
        for (int i = 0; i < 5; i++) {
            final InvoiceVerificationRecordEntity e = sample("IV-3" + i, "SN-3" + i, "0", BigDecimal.TEN);
            e.setVerificationTime(base.plusMinutes(i));
            repository.save(e);
        }

        assertThat(repository.findByVerificationTimeBetween(
                base.minusHours(1), base.plusHours(1), PageRequest.of(0, 3))
                .getTotalElements()).isEqualTo(5L);
    }

    @Test
    @DisplayName("countByVerificationResult counts rows for a given raw return code")
    void countByVerificationResult_countsByRawCode() {
        repository.save(sample("IV-40", "SN-40", "0", BigDecimal.TEN));
        repository.save(sample("IV-41", "SN-41", "0", BigDecimal.TEN));
        repository.save(sample("IV-42", "SN-42", "1", BigDecimal.TEN));

        assertThat(repository.countByVerificationResult("0")).isEqualTo(2L);
        assertThat(repository.countByVerificationResult("1")).isEqualTo(1L);
    }

    private static InvoiceVerificationRecordEntity sample(final String invoiceId,
                                                          final String serialNo,
                                                          final String result,
                                                          final BigDecimal amount) {
        final InvoiceVerificationRecordEntity e = new InvoiceVerificationRecordEntity();
        e.setInvoiceId(invoiceId);
        e.setInvoiceCode("123456789012");
        e.setInvoiceNumber("12345678");
        e.setInvoiceAmount(amount);
        e.setInvoiceDate(LocalDate.of(2026, 6, 1));
        e.setVerificationResult(result);
        e.setVerificationTime(LocalDateTime.of(2026, 6, 14, 10, 0));
        e.setFailureReason(null);
        e.setSerialNo(serialNo);
        // createdAt / updatedAt left null to exercise @PrePersist fallback
        return e;
    }
}
