package com.puchain.fep.processor.reconciliation;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.body.supplychain.HxqyInfo;
import com.puchain.fep.processor.body.supplychain.PzCheckQuery3107;
import com.puchain.fep.processor.body.supplychain.PzCheckQueryReturn3108;
import com.puchain.fep.processor.body.supplychain.PzCheckReturn;
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
 * Unit tests for {@link PlatformReconciliationService}.
 *
 * <p>Coverage（v1d Plan AC3 ≥ 8 cases，本类共 8 cases）：</p>
 * <ol>
 *   <li>initiateOutbound_shouldSavePending: happy path 落 PENDING 行</li>
 *   <li>initiateOutbound_dirMapMiss_shouldThrow: 3107/INFO_SERVICE_ORG 未注册</li>
 *   <li>initiateOutbound_invalidCheckDate_shouldThrowIAE: yyyyMMdd 解析失败</li>
 *   <li>processInbound_orphan3108_shouldThrowOrphanReturn: 找不到 3107 PENDING</li>
 *   <li>processInbound_alreadyPaired3107_shouldThrowDuplicateReturn: 已配对场景</li>
 *   <li>processInbound_completedPath_shouldCreateNewRowAndUpdate3107: declared==actual happy path</li>
 *   <li>processInbound_discrepancyPath_shouldCreateDiscrepancyRow: declared!=actual</li>
 *   <li>processInbound_dirMapMiss_shouldThrow: 3108/INFO_SERVICE_ORG 未注册</li>
 * </ol>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@DisplayName("PlatformReconciliationService: 3107 outbound + 3108 inbound pairing")
class PlatformReconciliationServiceTest {

    private InMemoryReconciliationStore store;
    private ReconciliationDiffCalculator calculator;
    private PlatformReconciliationService service;

    @BeforeEach
    void setUp() {
        store = new InMemoryReconciliationStore();
        calculator = new ReconciliationDiffCalculator();
        service = new PlatformReconciliationService(store, calculator);
    }

    @Test
    @DisplayName("initiateOutbound: happy path → save PENDING row with pairedSerialNo=null")
    void initiateOutbound_shouldSavePending() {
        final PzCheckQuery3107 body = sample3107("20260427", "3", 3);

        final ReconciliationRecord saved = service.initiateOutbound(body, "SN-OUT-1");

        assertThat(saved.getStatus()).isEqualTo("PENDING");
        assertThat(saved.getMessageType()).isEqualTo("3107");
        assertThat(saved.getSerialNo()).isEqualTo("SN-OUT-1");
        assertThat(saved.getPairedSerialNo()).isNull();
        assertThat(saved.getTotalTransactionCount()).isEqualTo(3);
        assertThat(saved.getActualCount()).isEqualTo(3);
        assertThat(saved.getDiscrepancyCount()).isZero();
        assertThat(saved.getReconciliationDate()).isEqualTo(LocalDate.of(2026, 4, 27));
        assertThat(saved.getReconciliationId()).matches("^RC_\\d{8}_\\d{3}$");

        final Optional<ReconciliationRecord> looked =
                store.findBySerialNoAndMessageType("SN-OUT-1", "3107");
        assertThat(looked).isPresent();
        assertThat(looked.get().getStatus()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("initiateOutbound: 3107/INFO_SERVICE_ORG miss → FepBusinessException(RECON_DIR_MAP_MISS)")
    void initiateOutbound_dirMapMiss_shouldThrow() {
        final PzCheckQuery3107 body = sample3107("20260427", "0", 0);

        try (MockedStatic<MessageDirectionMap> mocked =
                     Mockito.mockStatic(MessageDirectionMap.class)) {
            mocked.when(() -> MessageDirectionMap.lookup(
                            MessageType.MSG_3107, AccessRole.INFO_SERVICE_ORG))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.initiateOutbound(body, "SN-MISS-OUT"))
                    .isInstanceOf(FepBusinessException.class)
                    .hasMessageContaining("MessageDirectionMap miss for 3107/INFO_SERVICE_ORG")
                    .extracting(t -> ((FepBusinessException) t).getErrorCode())
                    .isEqualTo(FepErrorCode.RECON_DIR_MAP_MISS);
        }
    }

    @Test
    @DisplayName("initiateOutbound: invalid checkDate → IllegalArgumentException")
    void initiateOutbound_invalidCheckDate_shouldThrowIAE() {
        final PzCheckQuery3107 body = sample3107("2026-04-27", "0", 0);

        assertThatThrownBy(() -> service.initiateOutbound(body, "SN-BAD-DATE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("checkDate not yyyyMMdd")
                .hasMessageContaining("2026-04-27");
    }

    @Test
    @DisplayName("processInbound: no matching 3107 → FepBusinessException(RECON_ORPHAN_RETURN)")
    void processInbound_orphan3108_shouldThrowOrphanReturn() {
        final PzCheckQueryReturn3108 body = sample3108("20260427", "1", 1);

        assertThatThrownBy(() -> service.processInbound(body, "SN-ORPHAN"))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("orphan 3108")
                .hasMessageContaining("SN-ORPHAN")
                .extracting(t -> ((FepBusinessException) t).getErrorCode())
                .isEqualTo(FepErrorCode.RECON_ORPHAN_RETURN);
    }

    @Test
    @DisplayName("processInbound: 3107 already paired (status != PENDING) → RECON_DUPLICATE_RETURN")
    void processInbound_alreadyPaired3107_shouldThrowDuplicateReturn() {
        // 先 initiate 3107 (PENDING)
        service.initiateOutbound(sample3107("20260427", "1", 1), "SN-DUP");
        // 模拟已配对：手工 rebuild 为 COMPLETED
        final ReconciliationRecord pending = store
                .findBySerialNoAndMessageType("SN-DUP", "3107").orElseThrow();
        store.save(ReconciliationRecord.builder()
                .from(pending)
                .status("COMPLETED")
                .pairedSerialNo("SN-DUP")
                .build());

        final PzCheckQueryReturn3108 body = sample3108("20260427", "1", 1);

        assertThatThrownBy(() -> service.processInbound(body, "SN-DUP"))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("3107 already paired")
                .hasMessageContaining("SN-DUP")
                .extracting(t -> ((FepBusinessException) t).getErrorCode())
                .isEqualTo(FepErrorCode.RECON_DUPLICATE_RETURN);
    }

    @Test
    @DisplayName("processInbound: declared==actual → COMPLETED 3108 row + 3107 paired update")
    void processInbound_completedPath_shouldCreateNewRowAndUpdate3107() {
        service.initiateOutbound(sample3107("20260427", "2", 2), "SN-COMP");
        final PzCheckQueryReturn3108 body = sample3108("20260427", "2", 2);

        final ReconciliationOutcome outcome = service.processInbound(body, "SN-COMP");

        assertThat(outcome.status()).isEqualTo(ReconciliationStatus.COMPLETED);
        assertThat(outcome.discrepancyCount()).isZero();
        assertThat(outcome.declaredCount()).isEqualTo(2);
        assertThat(outcome.actualSize()).isEqualTo(2);

        // 3108 行新建
        final ReconciliationRecord row3108 = store
                .findBySerialNoAndMessageType("SN-COMP", "3108").orElseThrow();
        assertThat(row3108.getStatus()).isEqualTo("COMPLETED");
        assertThat(row3108.getPairedSerialNo()).isEqualTo("SN-COMP");
        assertThat(row3108.getActualCount()).isEqualTo(2);
        assertThat(row3108.getTotalTransactionCount()).isEqualTo(2);

        // 3107 行被 update（pairedSerialNo 写入），状态保持 PENDING
        final ReconciliationRecord row3107 = store
                .findBySerialNoAndMessageType("SN-COMP", "3107").orElseThrow();
        assertThat(row3107.getStatus()).isEqualTo("PENDING");
        assertThat(row3107.getPairedSerialNo()).isEqualTo("SN-COMP");
    }

    @Test
    @DisplayName("processInbound: declared != actual → DISCREPANCY 3108 row")
    void processInbound_discrepancyPath_shouldCreateDiscrepancyRow() {
        service.initiateOutbound(sample3107("20260427", "5", 5), "SN-DIFF");
        final PzCheckQueryReturn3108 body = sample3108("20260427", "5", 3);

        final ReconciliationOutcome outcome = service.processInbound(body, "SN-DIFF");

        assertThat(outcome.status()).isEqualTo(ReconciliationStatus.DISCREPANCY);
        assertThat(outcome.discrepancyCount()).isEqualTo(2);

        final ReconciliationRecord row3108 = store
                .findBySerialNoAndMessageType("SN-DIFF", "3108").orElseThrow();
        assertThat(row3108.getStatus()).isEqualTo("DISCREPANCY");
        assertThat(row3108.getDiscrepancyCount()).isEqualTo(2);
        assertThat(row3108.getActualCount()).isEqualTo(3);
        assertThat(row3108.getTotalTransactionCount()).isEqualTo(5);
    }

    @Test
    @DisplayName("processInbound: 3108/INFO_SERVICE_ORG miss → FepBusinessException(RECON_DIR_MAP_MISS)")
    void processInbound_dirMapMiss_shouldThrow() {
        final PzCheckQueryReturn3108 body = sample3108("20260427", "0", 0);

        try (MockedStatic<MessageDirectionMap> mocked =
                     Mockito.mockStatic(MessageDirectionMap.class)) {
            mocked.when(() -> MessageDirectionMap.lookup(
                            MessageType.MSG_3108, AccessRole.INFO_SERVICE_ORG))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.processInbound(body, "SN-MISS-IN"))
                    .isInstanceOf(FepBusinessException.class)
                    .hasMessageContaining("MessageDirectionMap miss for 3108/INFO_SERVICE_ORG")
                    .extracting(t -> ((FepBusinessException) t).getErrorCode())
                    .isEqualTo(FepErrorCode.RECON_DIR_MAP_MISS);
        }
    }

    /**
     * Build a {@link PzCheckQuery3107} body with the minimum fields required.
     *
     * @param checkDate  yyyyMMdd date literal landing in {@code reconciliationDate}
     * @param hxqyNum    string-form declared count parsed via {@link Integer#parseInt}
     * @param actualSize number of {@link HxqyInfo} placeholders to attach
     * @return populated body, never null
     */
    private PzCheckQuery3107 sample3107(
            final String checkDate, final String hxqyNum, final int actualSize) {
        final PzCheckQuery3107 body = new PzCheckQuery3107();
        body.setCheckDate(checkDate);
        body.setHxqyNum(hxqyNum);
        final List<HxqyInfo> details = new ArrayList<>(actualSize);
        for (int i = 0; i < actualSize; i++) {
            details.add(new HxqyInfo());
        }
        body.setHxqyInfo(details);
        return body;
    }

    /**
     * Build a {@link PzCheckQueryReturn3108} body with the minimum fields required.
     *
     * @param checkDate  yyyyMMdd date literal landing in {@code reconciliationDate}
     * @param hxqyNum    string-form declared count parsed via {@link Integer#parseInt}
     * @param actualSize number of {@link PzCheckReturn} placeholders to attach
     * @return populated body, never null
     */
    private PzCheckQueryReturn3108 sample3108(
            final String checkDate, final String hxqyNum, final int actualSize) {
        final PzCheckQueryReturn3108 body = new PzCheckQueryReturn3108();
        body.setCheckDate(checkDate);
        body.setHxqyNum(hxqyNum);
        final List<PzCheckReturn> returns = new ArrayList<>(actualSize);
        for (int i = 0; i < actualSize; i++) {
            returns.add(new PzCheckReturn());
        }
        body.setPzCheckReturn(returns);
        return body;
    }
}
