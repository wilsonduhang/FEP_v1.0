package com.puchain.fep.web.tracking;

import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.body.supplychain.QyAccQueryReturn3006;
import com.puchain.fep.processor.event.InboundMessageProcessedEvent;
import com.puchain.fep.web.integration.tracking.CorporateAccountRecordEntity;
import com.puchain.fep.web.integration.tracking.CorporateAccountRecordRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end wiring for the 3006 corporate account tracking write path
 * (§6.4.1 FR-DATA-DB-01).
 *
 * <p><strong>Coverage boundary (Plan MAJOR-2):</strong> publishes the
 * {@link InboundMessageProcessedEvent} directly, exercising the
 * listener→service→repo chain but not the {@code InboundMessageDispatcher.dispatch}
 * gate (covered by GHA strong regression). 3006 has no 9120-ack listener, so no
 * institution-code property is required.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@Transactional
@DisplayName("Corporate account tracking: event-published write path")
class CorporateAccountTrackingIntegrationTest {

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private CorporateAccountRecordRepository repository;

    @BeforeEach
    void cleanUp() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("publishing a 3006 event persists a mapped corporate account record")
    void publish3006Event_persistsMappedRecord() {
        final QyAccQueryReturn3006 body = account("USCI-IT-1", "SN-IT-1", "对公账户甲", "0");

        publisher.publishEvent(new InboundMessageProcessedEvent(
                MessageType.MSG_3006, "T0000001", "SN-IT-1", body, Instant.now()));

        final Optional<CorporateAccountRecordEntity> found =
                repository.findByEnterpriseId("USCI-IT-1");
        assertThat(found).isPresent();
        assertThat(found.get().getAccountName()).isEqualTo("对公账户甲");
        assertThat(found.get().getAccountStatus()).isEqualTo("0");
        assertThat(found.get().getSerialNo()).isEqualTo("SN-IT-1");
        assertThat(found.get().getCreatedAt()).isNotNull();
        // 3006 carries none of these → null
        assertThat(found.get().getAccountNumber()).isNull();
        assertThat(found.get().getOpeningBank()).isNull();
        assertThat(found.get().getAccountType()).isNull();
    }

    @Test
    @DisplayName("a re-verification for the same USCI upserts (latest status wins, no duplicate)")
    void reVerification_idempotentUpsert() {
        publisher.publishEvent(new InboundMessageProcessedEvent(
                MessageType.MSG_3006, "T0000001", "SN-IT-2a",
                account("USCI-IT-2", "SN-IT-2a", "对公账户乙", "0"), Instant.now()));
        publisher.publishEvent(new InboundMessageProcessedEvent(
                MessageType.MSG_3006, "T0000002", "SN-IT-2b",
                account("USCI-IT-2", "SN-IT-2b", "对公账户乙", "1"), Instant.now()));

        assertThat(repository.count()).isEqualTo(1L);
        final CorporateAccountRecordEntity row =
                repository.findByEnterpriseId("USCI-IT-2").orElseThrow();
        assertThat(row.getAccountStatus()).isEqualTo("1");
        assertThat(row.getSerialNo()).isEqualTo("SN-IT-2b");
    }

    private static QyAccQueryReturn3006 account(final String qyAccCode,
                                                final String serialNo,
                                                final String accName,
                                                final String returnCode) {
        final QyAccQueryReturn3006 b = new QyAccQueryReturn3006();
        b.setSerialNo(serialNo);
        b.setQyAccName(accName);
        b.setQyAccCode(qyAccCode);
        b.setAccReturnCode(returnCode);
        b.setAccReturnMemo("memo");
        return b;
    }
}
