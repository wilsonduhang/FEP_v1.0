package com.puchain.fep.processor.reconciliation;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.body.supplychain.BankCheckDay3116;
import com.puchain.fep.processor.body.supplychain.CheckDetailInfo;
import com.puchain.fep.processor.routing.AccessRole;
import com.puchain.fep.processor.routing.MessageDirectionMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link BankReconciliationService}.
 *
 * <p>Coverage（v1d Plan AC7 ≥ 7 cases，本类共 8 cases）：</p>
 * <ol>
 *   <li>completedPath: declared == actual.size → COMPLETED</li>
 *   <li>discrepancyPath: declared != actual.size → DISCREPANCY + diff</li>
 *   <li>checkDetailInfoNull: list null → actual=0</li>
 *   <li>checkDetailNumNonNumeric: parse 失败 → IllegalArgumentException</li>
 *   <li>blankSerialNo: empty/whitespace serialNo → IllegalArgumentException</li>
 *   <li>idGeneration: 序列 1 → "001" 三位补零</li>
 *   <li>dailyLimitExceeded: countByDate 999 → FepBusinessException with date literal</li>
 *   <li>directionMapMiss: lookup 返回 empty → FepBusinessException</li>
 * </ol>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@DisplayName("BankReconciliationService: 3116 inbound reconciliation")
class BankReconciliationServiceTest {

    private InMemoryReconciliationStore store;
    private ReconciliationDiffCalculator calculator;
    private BankReconciliationService service;

    @BeforeEach
    void setUp() {
        store = new InMemoryReconciliationStore();
        calculator = new ReconciliationDiffCalculator();
        service = new BankReconciliationService(store, calculator);
    }

    @Test
    @DisplayName("completedPath: declared equals actual size → COMPLETED + saved record")
    void completedPath_declaredEqualsActual_shouldSaveCompleted() {
        final BankCheckDay3116 body = sampleBody("20260427", "2", 2);

        final ReconciliationOutcome outcome = service.processInbound(body, "SN-COMPLETED-1");

        assertThat(outcome.status()).isEqualTo(ReconciliationStatus.COMPLETED);
        assertThat(outcome.discrepancyCount()).isZero();
        assertThat(outcome.declaredCount()).isEqualTo(2);
        assertThat(outcome.actualSize()).isEqualTo(2);

        final Optional<ReconciliationRecord> saved =
                store.findBySerialNoAndMessageType("SN-COMPLETED-1", "3116");
        assertThat(saved).isPresent();
        assertThat(saved.get().getStatus()).isEqualTo("COMPLETED");
        assertThat(saved.get().getReconciliationDate()).isEqualTo(LocalDate.of(2026, 4, 27));
        assertThat(saved.get().getActualCount()).isEqualTo(2);
        assertThat(saved.get().getTotalTransactionCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("discrepancyPath: declared != actual size → DISCREPANCY with abs diff")
    void discrepancyPath_declaredNotEqualActual_shouldSaveDiscrepancy() {
        final BankCheckDay3116 body = sampleBody("20260427", "5", 3);

        final ReconciliationOutcome outcome = service.processInbound(body, "SN-DISCREPANCY-1");

        assertThat(outcome.status()).isEqualTo(ReconciliationStatus.DISCREPANCY);
        assertThat(outcome.discrepancyCount()).isEqualTo(2);

        final ReconciliationRecord saved =
                store.findBySerialNoAndMessageType("SN-DISCREPANCY-1", "3116").orElseThrow();
        assertThat(saved.getStatus()).isEqualTo("DISCREPANCY");
        assertThat(saved.getDiscrepancyCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("checkDetailInfo null → actual count treated as 0")
    void checkDetailInfoNull_shouldUseZeroActual() {
        final BankCheckDay3116 body = sampleBody("20260427", "0", 0);
        body.setCheckDetailInfo(null);

        final ReconciliationOutcome outcome = service.processInbound(body, "SN-NULL-1");

        assertThat(outcome.status()).isEqualTo(ReconciliationStatus.COMPLETED);
        assertThat(outcome.actualSize()).isZero();
        assertThat(outcome.declaredCount()).isZero();
    }

    @Test
    @DisplayName("checkDetailNum non-numeric → IllegalArgumentException")
    void checkDetailNumNonNumeric_shouldThrowIAE() {
        final BankCheckDay3116 body = sampleBody("20260427", "ABC", 0);

        assertThatThrownBy(() -> service.processInbound(body, "SN-IAE-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("checkDetailNum not numeric")
                .hasMessageContaining("ABC");
    }

    @Test
    @DisplayName("blank serialNo → IllegalArgumentException")
    void blankSerialNo_shouldThrowIAE() {
        final BankCheckDay3116 body = sampleBody("20260427", "0", 0);

        assertThatThrownBy(() -> service.processInbound(body, "  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("serialNo");

        assertThatThrownBy(() -> service.processInbound(body, ""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("id generation: first record of the day → suffix '001'")
    void idGeneration_shouldFormatRC_YYYYMMDD_NNN() {
        final BankCheckDay3116 body = sampleBody("20260427", "1", 1);

        service.processInbound(body, "SN-ID-1");

        final ReconciliationRecord saved =
                store.findBySerialNoAndMessageType("SN-ID-1", "3116").orElseThrow();
        // RC_<reconciliationDate's today>_001 — store.countByDate uses LocalDate.now() which
        // reflects today, not body.checkDate. Verify only the suffix shape.
        assertThat(saved.getReconciliationId())
                .matches("^RC_\\d{8}_001$");
    }

    @Test
    @DisplayName("daily limit exceeded: countByDate returns 999 → FepBusinessException with date in message")
    void dailyLimitExceeded_shouldThrow() {
        final ReconciliationStore stubStore = new ReconciliationStore() {
            @Override
            public ReconciliationRecord save(final ReconciliationRecord record) {
                return record;
            }

            @Override
            public Optional<ReconciliationRecord> findBySerialNoAndMessageType(
                    final String serialNo, final String messageType) {
                return Optional.empty();
            }

            @Override
            public List<ReconciliationRecord> findByDateAndStatus(
                    final LocalDate date, final String status) {
                return List.of();
            }

            @Override
            public long countByDate(final LocalDate date) {
                return 999L;
            }
        };
        final BankReconciliationService boundedService =
                new BankReconciliationService(stubStore, calculator);
        final BankCheckDay3116 body = sampleBody("20260427", "0", 0);

        final LocalDate today = LocalDate.now();
        assertThatThrownBy(() -> boundedService.processInbound(body, "SN-LIMIT-1"))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("daily reconciliation limit exceeded")
                .hasMessageContaining(today.toString())
                .extracting(t -> ((FepBusinessException) t).getErrorCode())
                .isEqualTo(FepErrorCode.RECON_DAILY_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("MessageDirectionMap miss → FepBusinessException(RECON_DIR_MAP_MISS)")
    void directionMapMiss_shouldThrow() {
        final BankCheckDay3116 body = sampleBody("20260427", "0", 0);

        try (MockedStatic<MessageDirectionMap> mocked =
                     Mockito.mockStatic(MessageDirectionMap.class)) {
            mocked.when(() -> MessageDirectionMap.lookup(
                            MessageType.MSG_3116, AccessRole.ACCEPTING_ORG))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.processInbound(body, "SN-MISS-1"))
                    .isInstanceOf(FepBusinessException.class)
                    .hasMessageContaining("MessageDirectionMap miss for 3116/ACCEPTING_ORG")
                    .extracting(t -> ((FepBusinessException) t).getErrorCode())
                    .isEqualTo(FepErrorCode.RECON_DIR_MAP_MISS);
        }
    }

    /**
     * Build a {@link BankCheckDay3116} body with the minimum fields required by
     * {@link BankReconciliationService#processInbound}.
     *
     * @param checkDate      yyyyMMdd date literal landing in {@code reconciliationDate}
     * @param checkDetailNum string-form declared count parsed via {@link Integer#parseInt}
     * @param actualSize     number of {@link CheckDetailInfo} placeholders to attach
     * @return populated body, never null
     */
    private BankCheckDay3116 sampleBody(
            final String checkDate, final String checkDetailNum, final int actualSize) {
        final BankCheckDay3116 body = new BankCheckDay3116();
        body.setCheckDate(checkDate);
        body.setCheckDetailNum(checkDetailNum);
        final List<CheckDetailInfo> details = new ArrayList<>(actualSize);
        for (int i = 0; i < actualSize; i++) {
            details.add(new CheckDetailInfo());
        }
        body.setCheckDetailInfo(details);
        return body;
    }
}
