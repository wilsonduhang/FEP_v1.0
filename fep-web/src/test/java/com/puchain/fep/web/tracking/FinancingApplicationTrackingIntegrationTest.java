package com.puchain.fep.web.tracking;

import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.body.supplychain.RzAmtInfo3009;
import com.puchain.fep.processor.body.supplychain.RzReturnInfo3009;
import com.puchain.fep.processor.event.InboundMessageProcessedEvent;
import com.puchain.fep.web.integration.tracking.FinancingApplicationRecordEntity;
import com.puchain.fep.web.integration.tracking.FinancingApplicationRecordRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end wiring for the 3009 financing application tracking write path
 * (§6.4.1 FR-DATA-DB-01).
 *
 * <p><strong>Coverage boundary (Plan MAJOR-2):</strong> publishes the
 * {@link InboundMessageProcessedEvent} directly, exercising the
 * listener→service→repo chain but not the {@code InboundMessageDispatcher.dispatch}
 * {@code @Transactional} + {@code status==COMPLETED} gate (covered by GHA strong
 * regression).</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = "fep.collector.institution-code=A1000143000999")
@DisplayName("Financing application tracking: event-published write path")
class FinancingApplicationTrackingIntegrationTest {

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private FinancingApplicationRecordRepository repository;

    @BeforeEach
    void cleanUp() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("publishing a 3009 event persists a mapped financing application record")
    void publish3009Event_persistsMappedRecord() {
        final RzReturnInfo3009 body = financing("APP-IT-1", "SN-IT-1", "1", "60000.00", "58000.00");

        publisher.publishEvent(new InboundMessageProcessedEvent(
                MessageType.MSG_3009, "T0000001", "SN-IT-1", body, Instant.now()));

        final Optional<FinancingApplicationRecordEntity> found =
                repository.findByApplicationId("APP-IT-1");
        assertThat(found).isPresent();
        assertThat(found.get().getCoreEnterpriseName()).isEqualTo("核心企业甲");
        assertThat(found.get().getApprovalStatus()).isEqualTo("1");
        assertThat(found.get().getApplicationAmount()).isEqualByComparingTo("60000.00");
        assertThat(found.get().getApprovalAmount()).isEqualByComparingTo("58000.00");
        assertThat(found.get().getSerialNo()).isEqualTo("SN-IT-1");
        assertThat(found.get().getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("a later phase for the same application upserts (latest phase wins, no duplicate)")
    void laterPhase_idempotentUpsert() {
        publisher.publishEvent(new InboundMessageProcessedEvent(
                MessageType.MSG_3009, "T0000001", "SN-IT-2a",
                financing("APP-IT-2", "SN-IT-2a", "1", "100.00", "90.00"), Instant.now()));
        publisher.publishEvent(new InboundMessageProcessedEvent(
                MessageType.MSG_3009, "T0000002", "SN-IT-2b",
                financing("APP-IT-2", "SN-IT-2b", "3", "100.00", "95.00"), Instant.now()));

        assertThat(repository.count()).isEqualTo(1L);
        final FinancingApplicationRecordEntity row =
                repository.findByApplicationId("APP-IT-2").orElseThrow();
        assertThat(row.getApprovalStatus()).isEqualTo("3");
        assertThat(row.getSerialNo()).isEqualTo("SN-IT-2b");
    }

    @Test
    @DisplayName("absent rzAmtInfo → amounts stored as null (no NPE, no rollback)")
    void absentAmtInfo_amountsNull() {
        final RzReturnInfo3009 body = new RzReturnInfo3009();
        body.setSerialNo("SN-IT-3");
        body.setPlatApplyNo("APP-IT-3");
        body.setHxqyName("核心企业丙");
        body.setRzpzNo("RZPZ-3");
        body.setRzPhaseCode("1");
        // rzAmtInfo intentionally left null

        publisher.publishEvent(new InboundMessageProcessedEvent(
                MessageType.MSG_3009, "T0000003", "SN-IT-3", body, Instant.now()));

        final FinancingApplicationRecordEntity row =
                repository.findByApplicationId("APP-IT-3").orElseThrow();
        assertThat(row.getApplicationAmount()).isNull();
        assertThat(row.getApprovalAmount()).isNull();
        assertThat(row.getApprovalStatus()).isEqualTo("1");
    }

    private static RzReturnInfo3009 financing(final String platApplyNo,
                                              final String serialNo,
                                              final String phaseCode,
                                              final String rzAmt,
                                              final String rzNetAmt) {
        final RzReturnInfo3009 b = new RzReturnInfo3009();
        b.setSerialNo(serialNo);
        b.setPlatApplyNo(platApplyNo);
        b.setHxqyName("核心企业甲");
        b.setRzpzNo("RZPZ-1");
        b.setRzPhaseCode(phaseCode);
        b.setRzPhaseInfo("阶段说明");
        final RzAmtInfo3009 amt = new RzAmtInfo3009();
        amt.setRzAmt(rzAmt);
        amt.setRzNetAmt(rzNetAmt);
        b.setRzAmtInfo(amt);
        return b;
    }
}
