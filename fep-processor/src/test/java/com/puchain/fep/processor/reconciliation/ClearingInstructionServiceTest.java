package com.puchain.fep.processor.reconciliation;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.body.supplychain.PlatPay3115;
import com.puchain.fep.processor.body.supplychain.QsInfo;
import com.puchain.fep.processor.body.supplychain.QsReturnInfo;
import com.puchain.fep.processor.routing.AccessRole;
import com.puchain.fep.processor.routing.MessageDirectionMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ClearingInstructionService}.
 *
 * <p>Coverage（v1d Plan AC3 ≥ 10 cases，本类共 14 cases）：</p>
 * <ol>
 *   <li>initiateOutbound_dirMapMiss_3115_outbound_shouldThrow</li>
 *   <li>initiateOutbound_pk7SignElement_shouldThrow（Mode E 守护）</li>
 *   <li>initiateOutbound_pk7QsfqSign_shouldThrow（Mode E 守护）</li>
 *   <li>initiateOutbound_pk7PlatSign_shouldThrow（Mode E 守护）</li>
 *   <li>initiateOutbound_qsInfoBusinessRuleViolation_shouldThrow</li>
 *   <li>initiateOutbound_validBody_shouldSaveAllPending</li>
 *   <li>initiateOutbound_blankMessageId_shouldThrowIAE</li>
 *   <li>initiateOutbound_dataIntegrityViolation_shouldThrowDuplicate</li>
 *   <li>processInboundReturn_dirMapMiss_3115_inbound_shouldThrow</li>
 *   <li>processInboundReturn_qsReturnInfoNull_shouldSkip</li>
 *   <li>processInboundReturn_orphanQs_shouldThrowOrphanReturn</li>
 *   <li>processInboundReturn_returnCode0_shouldSetSuccess</li>
 *   <li>processInboundReturn_returnCode00_shouldSetSuccess</li>
 *   <li>processInboundReturn_returnCodeOther_shouldSetFailed</li>
 *   <li>processInboundReturn_failedNullMemo_shouldSetUnknown</li>
 *   <li>processInboundReturn_rebuilt_shouldKeepImmutableFields</li>
 * </ol>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@DisplayName("ClearingInstructionService: 3115 outbound + inbound return")
class ClearingInstructionServiceTest {

    private InMemoryClearingInstructionStore store;
    private ReconciliationDiffCalculator calculator;
    private ClearingInstructionService service;

    @BeforeEach
    void setUp() {
        store = new InMemoryClearingInstructionStore();
        calculator = new ReconciliationDiffCalculator();
        service = new ClearingInstructionService(store, calculator);
    }

    // ============ initiateOutbound (8 cases) ============

    @Test
    @DisplayName("initiateOutbound: 3115/ACCEPTING_ORG miss → FepBusinessException(RECON_DIR_MAP_MISS)")
    void initiateOutbound_dirMapMiss_3115_outbound_shouldThrow() {
        final PlatPay3115 body = sampleOutbound("PP-001", List.of(qs("QS-1", "100.00")));

        try (MockedStatic<MessageDirectionMap> mocked =
                     Mockito.mockStatic(MessageDirectionMap.class)) {
            mocked.when(() -> MessageDirectionMap.lookup(
                            MessageType.MSG_3115, AccessRole.ACCEPTING_ORG))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.initiateOutbound(body, "MSG-1"))
                    .isInstanceOf(FepBusinessException.class)
                    .hasMessageContaining("MessageDirectionMap miss for 3115/ACCEPTING_ORG")
                    .extracting(t -> ((FepBusinessException) t).getErrorCode())
                    .isEqualTo(FepErrorCode.RECON_DIR_MAP_MISS);
        }
    }

    @Test
    @DisplayName("initiateOutbound: SignElement non-null → CLEAR_BUSINESS_RULE_VIOLATION (Mode E)")
    void initiateOutbound_pk7SignElement_shouldThrow() {
        final PlatPay3115 body = sampleOutbound("PP-PK7-1", List.of(qs("QS-1", "100.00")));
        body.setSignElement("base64-bytes-placeholder");

        assertThatThrownBy(() -> service.initiateOutbound(body, "MSG-PK7-1"))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("PK7 fields")
                .hasMessageContaining("security integration TBD")
                .extracting(t -> ((FepBusinessException) t).getErrorCode())
                .isEqualTo(FepErrorCode.CLEAR_BUSINESS_RULE_VIOLATION);
        // 守护早于 store 接触
        assertThat(store.findByMessageId("MSG-PK7-1")).isEmpty();
    }

    @Test
    @DisplayName("initiateOutbound: qsfqSign non-null → CLEAR_BUSINESS_RULE_VIOLATION (Mode E)")
    void initiateOutbound_pk7QsfqSign_shouldThrow() {
        final PlatPay3115 body = sampleOutbound("PP-PK7-2", List.of(qs("QS-1", "100.00")));
        body.setQsfqSign("sm2-signature-bytes");

        assertThatThrownBy(() -> service.initiateOutbound(body, "MSG-PK7-2"))
                .isInstanceOf(FepBusinessException.class)
                .extracting(t -> ((FepBusinessException) t).getErrorCode())
                .isEqualTo(FepErrorCode.CLEAR_BUSINESS_RULE_VIOLATION);
    }

    @Test
    @DisplayName("initiateOutbound: PlatSign non-null → CLEAR_BUSINESS_RULE_VIOLATION (Mode E)")
    void initiateOutbound_pk7PlatSign_shouldThrow() {
        final PlatPay3115 body = sampleOutbound("PP-PK7-3", List.of(qs("QS-1", "100.00")));
        body.setPlatSign("plat-signature-bytes");

        assertThatThrownBy(() -> service.initiateOutbound(body, "MSG-PK7-3"))
                .isInstanceOf(FepBusinessException.class)
                .extracting(t -> ((FepBusinessException) t).getErrorCode())
                .isEqualTo(FepErrorCode.CLEAR_BUSINESS_RULE_VIOLATION);
    }

    @Test
    @DisplayName("initiateOutbound: amt invalid → CLEAR_BUSINESS_RULE_VIOLATION + nothing saved")
    void initiateOutbound_qsInfoBusinessRuleViolation_shouldThrow() {
        // 含一条 amt="0" 违例 + 一条合规
        final PlatPay3115 body = sampleOutbound("PP-RULE", List.of(
                qs("QS-1", "100.00"),
                qs("QS-2", "0")));

        assertThatThrownBy(() -> service.initiateOutbound(body, "MSG-RULE"))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("3115 outbound rejected")
                .hasMessageContaining("qsInfo violations")
                .extracting(t -> ((FepBusinessException) t).getErrorCode())
                .isEqualTo(FepErrorCode.CLEAR_BUSINESS_RULE_VIOLATION);
        // 全量拒绝：未落库
        assertThat(store.findByMessageId("MSG-RULE")).isEmpty();
    }

    @Test
    @DisplayName("initiateOutbound: valid body → save all PENDING rows")
    void initiateOutbound_validBody_shouldSaveAllPending() {
        final PlatPay3115 body = sampleOutbound("PP-OK", List.of(
                qs("QS-A", "150.00"),
                qs("QS-B", "200.50"),
                qs("QS-C", "0.01")));

        final List<ClearingInstructionRecord> saved = service.initiateOutbound(body, "MSG-OK");

        assertThat(saved).hasSize(3);
        assertThat(saved).allSatisfy(r -> {
            assertThat(r.getInstructionId()).isEqualTo("PP-OK");
            assertThat(r.getInstructionType()).isEqualTo("NORMAL");
            assertThat(r.getInstructionStatus())
                    .isEqualTo(ClearingInstructionStatus.PENDING.name());
            assertThat(r.getMessageId()).isEqualTo("MSG-OK");
            assertThat(r.getExecutionTime()).isNull();
            assertThat(r.getFailureCause()).isNull();
            assertThat(r.getCreatedAt()).isNotNull();
            assertThat(r.getUpdatedAt()).isNotNull();
        });
        assertThat(saved.get(0).getQsSerialNo()).isEqualTo("QS-A");
        assertThat(saved.get(0).getSettlementAmount()).isEqualByComparingTo("150.00");
        assertThat(saved.get(2).getSettlementAmount()).isEqualByComparingTo("0.01");
        // 落库 verified through store
        assertThat(store.findByMessageId("MSG-OK")).hasSize(3);
    }

    @Test
    @DisplayName("initiateOutbound: blank messageId → IllegalArgumentException")
    void initiateOutbound_blankMessageId_shouldThrowIAE() {
        final PlatPay3115 body = sampleOutbound("PP-BLANK", List.of(qs("QS-1", "1.00")));

        assertThatThrownBy(() -> service.initiateOutbound(body, "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("messageId");
    }

    @Test
    @DisplayName("initiateOutbound: store.save throws DataIntegrityViolationException → CLEAR_DUPLICATE_INSTRUCTION")
    void initiateOutbound_dataIntegrityViolation_shouldThrowDuplicate() {
        final ClearingInstructionStore mockStore = mock(ClearingInstructionStore.class);
        when(mockStore.save(any(ClearingInstructionRecord.class)))
                .thenThrow(new DataIntegrityViolationException("uk_clearing_pk"));
        final ClearingInstructionService svc = new ClearingInstructionService(mockStore, calculator);
        final PlatPay3115 body = sampleOutbound("PP-DUP", List.of(qs("QS-DUP", "100.00")));

        assertThatThrownBy(() -> svc.initiateOutbound(body, "MSG-DUP"))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("3115 platPayNo+qsSerialNo duplicate")
                .hasMessageContaining("PP-DUP/QS-DUP")
                .extracting(t -> ((FepBusinessException) t).getErrorCode())
                .isEqualTo(FepErrorCode.CLEAR_DUPLICATE_INSTRUCTION);
    }

    // ============ processInboundReturn (8 cases) ============

    @Test
    @DisplayName("processInboundReturn: 3115/INFO_SERVICE_ORG miss → RECON_DIR_MAP_MISS")
    void processInboundReturn_dirMapMiss_3115_inbound_shouldThrow() {
        final PlatPay3115 body = sampleOutbound("PP-IN-MISS", List.of(qs("QS-1", "100.00")));

        try (MockedStatic<MessageDirectionMap> mocked =
                     Mockito.mockStatic(MessageDirectionMap.class)) {
            mocked.when(() -> MessageDirectionMap.lookup(
                            MessageType.MSG_3115, AccessRole.INFO_SERVICE_ORG))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.processInboundReturn(body))
                    .isInstanceOf(FepBusinessException.class)
                    .hasMessageContaining("MessageDirectionMap miss for 3115/INFO_SERVICE_ORG")
                    .extracting(t -> ((FepBusinessException) t).getErrorCode())
                    .isEqualTo(FepErrorCode.RECON_DIR_MAP_MISS);
        }
    }

    @Test
    @DisplayName("processInboundReturn: qsReturnInfo null → skip (no update)")
    void processInboundReturn_qsReturnInfoNull_shouldSkip() {
        // 先 outbound 落 PENDING
        service.initiateOutbound(
                sampleOutbound("PP-SKIP", List.of(qs("QS-S1", "100.00"))),
                "MSG-SKIP-OUT");

        // 再传同一个 body（qsReturnInfo 全 null）
        final PlatPay3115 inbound = sampleOutbound("PP-SKIP", List.of(qs("QS-S1", "100.00")));

        final List<ClearingInstructionRecord> updated = service.processInboundReturn(inbound);

        assertThat(updated).isEmpty();
        // 既有行未变
        final Optional<ClearingInstructionRecord> existing =
                store.findByInstructionIdAndQsSerialNo("PP-SKIP", "QS-S1");
        assertThat(existing).isPresent();
        assertThat(existing.get().getInstructionStatus())
                .isEqualTo(ClearingInstructionStatus.PENDING.name());
    }

    @Test
    @DisplayName("processInboundReturn: no matching PENDING row → RECON_ORPHAN_RETURN")
    void processInboundReturn_orphanQs_shouldThrowOrphanReturn() {
        final PlatPay3115 body = sampleOutbound("PP-ORPHAN", List.of(qs("QS-NX", "100.00")));
        attachReturn(body, 0, "0", "ok");

        assertThatThrownBy(() -> service.processInboundReturn(body))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("orphan 3115 return")
                .hasMessageContaining("PP-ORPHAN")
                .hasMessageContaining("QS-NX")
                .extracting(t -> ((FepBusinessException) t).getErrorCode())
                .isEqualTo(FepErrorCode.RECON_ORPHAN_RETURN);
    }

    @Test
    @DisplayName("processInboundReturn: returnCode='0' → SUCCESS, failureCause=null")
    void processInboundReturn_returnCode0_shouldSetSuccess() {
        service.initiateOutbound(
                sampleOutbound("PP-S0", List.of(qs("QS-S0", "100.00"))),
                "MSG-S0-OUT");

        final PlatPay3115 inbound = sampleOutbound("PP-S0", List.of(qs("QS-S0", "100.00")));
        attachReturn(inbound, 0, "0", null);

        final List<ClearingInstructionRecord> updated = service.processInboundReturn(inbound);

        assertThat(updated).hasSize(1);
        assertThat(updated.get(0).getInstructionStatus())
                .isEqualTo(ClearingInstructionStatus.SUCCESS.name());
        assertThat(updated.get(0).getFailureCause()).isNull();
        assertThat(updated.get(0).getExecutionTime()).isNotNull();
    }

    @Test
    @DisplayName("processInboundReturn: returnCode='00' → SUCCESS, failureCause=null")
    void processInboundReturn_returnCode00_shouldSetSuccess() {
        service.initiateOutbound(
                sampleOutbound("PP-S00", List.of(qs("QS-S00", "100.00"))),
                "MSG-S00-OUT");

        final PlatPay3115 inbound = sampleOutbound("PP-S00", List.of(qs("QS-S00", "100.00")));
        attachReturn(inbound, 0, "00", "irrelevant memo");

        final List<ClearingInstructionRecord> updated = service.processInboundReturn(inbound);

        assertThat(updated).hasSize(1);
        assertThat(updated.get(0).getInstructionStatus())
                .isEqualTo(ClearingInstructionStatus.SUCCESS.name());
        // SUCCESS 路径 failureCause 强制 null（不取 memo）
        assertThat(updated.get(0).getFailureCause()).isNull();
    }

    @Test
    @DisplayName("processInboundReturn: returnCode='99' → FAILED, failureCause=memo")
    void processInboundReturn_returnCodeOther_shouldSetFailed() {
        service.initiateOutbound(
                sampleOutbound("PP-F99", List.of(qs("QS-F99", "100.00"))),
                "MSG-F99-OUT");

        final PlatPay3115 inbound = sampleOutbound("PP-F99", List.of(qs("QS-F99", "100.00")));
        attachReturn(inbound, 0, "99", "余额不足");

        final List<ClearingInstructionRecord> updated = service.processInboundReturn(inbound);

        assertThat(updated).hasSize(1);
        assertThat(updated.get(0).getInstructionStatus())
                .isEqualTo(ClearingInstructionStatus.FAILED.name());
        assertThat(updated.get(0).getFailureCause()).isEqualTo("余额不足");
        assertThat(updated.get(0).getExecutionTime()).isNotNull();
    }

    @Test
    @DisplayName("processInboundReturn: FAILED with null memo → failureCause='unknown'")
    void processInboundReturn_failedNullMemo_shouldSetUnknown() {
        service.initiateOutbound(
                sampleOutbound("PP-FNULL", List.of(qs("QS-FN", "100.00"))),
                "MSG-FNULL-OUT");

        final PlatPay3115 inbound = sampleOutbound("PP-FNULL", List.of(qs("QS-FN", "100.00")));
        attachReturn(inbound, 0, "01", null);

        final List<ClearingInstructionRecord> updated = service.processInboundReturn(inbound);

        assertThat(updated).hasSize(1);
        assertThat(updated.get(0).getInstructionStatus())
                .isEqualTo(ClearingInstructionStatus.FAILED.name());
        assertThat(updated.get(0).getFailureCause()).isEqualTo("unknown");
    }

    @Test
    @DisplayName("processInboundReturn: rebuild preserves immutable fields (qsSerialNo / settlementAmount / messageId / createdAt)")
    void processInboundReturn_rebuilt_shouldKeepImmutableFields() {
        final List<ClearingInstructionRecord> outboundSaved = service.initiateOutbound(
                sampleOutbound("PP-KEEP", List.of(qs("QS-KEEP", "12345.67"))),
                "MSG-KEEP-OUT");
        final ClearingInstructionRecord original = outboundSaved.get(0);

        final PlatPay3115 inbound = sampleOutbound("PP-KEEP", List.of(qs("QS-KEEP", "12345.67")));
        attachReturn(inbound, 0, "0", null);

        final List<ClearingInstructionRecord> updated = service.processInboundReturn(inbound);

        assertThat(updated).hasSize(1);
        final ClearingInstructionRecord rebuilt = updated.get(0);
        // 不可变字段保留
        assertThat(rebuilt.getInstructionId()).isEqualTo(original.getInstructionId());
        assertThat(rebuilt.getQsSerialNo()).isEqualTo(original.getQsSerialNo());
        assertThat(rebuilt.getInstructionType()).isEqualTo(original.getInstructionType());
        assertThat(rebuilt.getSettlementAmount())
                .isEqualByComparingTo(original.getSettlementAmount());
        assertThat(rebuilt.getPayerAccount()).isEqualTo(original.getPayerAccount());
        assertThat(rebuilt.getPayeeAccount()).isEqualTo(original.getPayeeAccount());
        assertThat(rebuilt.getMessageId()).isEqualTo(original.getMessageId());
        assertThat(rebuilt.getCreatedAt()).isEqualTo(original.getCreatedAt());
        // 变更字段
        assertThat(rebuilt.getInstructionStatus())
                .isEqualTo(ClearingInstructionStatus.SUCCESS.name());
        assertThat(rebuilt.getExecutionTime()).isNotNull();
        assertThat(rebuilt.getUpdatedAt()).isNotNull();
    }

    // ============ Helpers ============

    /**
     * Build a {@link PlatPay3115} body with the minimum fields for outbound flow.
     *
     * @param platPayNo {@code PlatPayNo}, used as instructionId
     * @param qsList    list of {@link QsInfo} children
     * @return populated body, never null
     */
    private PlatPay3115 sampleOutbound(final String platPayNo, final List<QsInfo> qsList) {
        final PlatPay3115 body = new PlatPay3115();
        body.setSerialNo("SN-" + platPayNo);
        body.setSendNodeCode("A1000143000104");
        body.setDesNodeCode("A1000143000105");
        body.setPlatPayNo(platPayNo);
        body.setHxqyName("核心企业 X");
        body.setHxqyCode("91110000000000000X");
        body.setQsInfo(new ArrayList<>(qsList));
        return body;
    }

    /**
     * Build a {@link QsInfo} with the minimum fields for outbound + inbound flow.
     *
     * @param qsSerialNo clearing serial number
     * @param amt        settlement amount, BigDecimal-parseable
     * @return populated qs info
     */
    private QsInfo qs(final String qsSerialNo, final String amt) {
        final QsInfo q = new QsInfo();
        q.setQsSerialNo(qsSerialNo);
        q.setAmt(amt);
        q.setFkfAccNo("6228480000000001");
        q.setSkfAccNo("6228480000000002");
        return q;
    }

    /**
     * Attach a {@link QsReturnInfo} payload to the qsInfo at given index.
     *
     * @param body  outbound body to mutate
     * @param idx   qsInfo index (0-based)
     * @param code  qsReturnCode literal
     * @param memo  qsReturnMemo (nullable)
     */
    private void attachReturn(final PlatPay3115 body, final int idx, final String code, final String memo) {
        final QsReturnInfo ret = new QsReturnInfo();
        ret.setQsReturnCode(code);
        ret.setQsReturnMemo(memo);
        body.getQsInfo().get(idx).setQsReturnInfo(ret);
    }
}
