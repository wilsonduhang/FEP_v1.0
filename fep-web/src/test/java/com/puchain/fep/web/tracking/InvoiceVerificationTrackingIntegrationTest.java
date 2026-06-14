package com.puchain.fep.web.tracking;

import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.body.supplychain.InvoCheckReturn3008;
import com.puchain.fep.processor.event.InboundMessageProcessedEvent;
import com.puchain.fep.web.integration.tracking.InvoiceVerificationRecordEntity;
import com.puchain.fep.web.integration.tracking.InvoiceVerificationRecordRepository;

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
 * End-to-end wiring for the 3008 invoice verification tracking write path:
 * {@code ApplicationEventPublisher} → {@link com.puchain.fep.web.tracking.listener.InvoiceVerificationEventListener}
 * → {@link com.puchain.fep.web.tracking.service.InvoiceVerificationTrackingService}
 * → {@link InvoiceVerificationRecordRepository} (§6.4.1 FR-DATA-DB-01).
 *
 * <p><strong>Coverage boundary (Plan MAJOR-2):</strong> this test publishes the
 * {@link InboundMessageProcessedEvent} directly, so it exercises the
 * listener→service→repo chain but <em>not</em> the
 * {@code InboundMessageDispatcher.dispatch} {@code @Transactional} +
 * {@code status==COMPLETED} gate that actually emits the event in production.
 * The production trigger (a real 3008 CFX envelope flowing through
 * {@code dispatch()}) is covered by the GHA strong-regression suite; this unit
 * boundary is documented here intentionally rather than duplicating an
 * envelope-level dispatcher IT.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@Transactional
@DisplayName("Invoice verification tracking: event-published write path")
class InvoiceVerificationTrackingIntegrationTest {

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private InvoiceVerificationRecordRepository repository;

    @BeforeEach
    void cleanUp() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("publishing a 3008 event persists a mapped invoice verification record")
    void publish3008Event_persistsMappedRecord() {
        final InvoCheckReturn3008 body = new InvoCheckReturn3008();
        body.setSerialNo("SN-IT-1");
        body.setInvoCode("123456789012");
        body.setInvoNum("87654321");
        body.setInvoAmt("2500.00");
        body.setInvoDate("20260601");
        body.setInvoCheckReturnCode("0");
        body.setInvoCheckReturnMemo(null);

        publisher.publishEvent(new InboundMessageProcessedEvent(
                MessageType.MSG_3008, "T0000001", "SN-IT-1", body, Instant.now()));

        final Optional<InvoiceVerificationRecordEntity> found =
                repository.findBySerialNo("SN-IT-1");
        assertThat(found).isPresent();
        assertThat(found.get().getInvoiceCode()).isEqualTo("123456789012");
        assertThat(found.get().getInvoiceNumber()).isEqualTo("87654321");
        assertThat(found.get().getInvoiceAmount()).isEqualByComparingTo("2500.00");
        assertThat(found.get().getInvoiceDate()).isEqualTo("2026-06-01");
        assertThat(found.get().getVerificationResult()).isEqualTo("0");
        assertThat(found.get().getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("a repeated 3008 for the same serial updates idempotently (no duplicate)")
    void repeatedEvent_idempotentUpsert() {
        final InvoCheckReturn3008 first = invoice("SN-IT-2", "0", "100.00");
        publisher.publishEvent(new InboundMessageProcessedEvent(
                MessageType.MSG_3008, "T0000001", "SN-IT-2", first, Instant.now()));

        final InvoCheckReturn3008 second = invoice("SN-IT-2", "1", "200.00");
        publisher.publishEvent(new InboundMessageProcessedEvent(
                MessageType.MSG_3008, "T0000002", "SN-IT-2", second, Instant.now()));

        assertThat(repository.count()).isEqualTo(1L);
        assertThat(repository.findBySerialNo("SN-IT-2"))
                .get()
                .extracting(InvoiceVerificationRecordEntity::getVerificationResult)
                .isEqualTo("1");
    }

    @Test
    @DisplayName("malformed invoAmt / invoDate are stored as null (defensive parse, no rollback)")
    void malformedAmountAndDate_storedAsNull() {
        final InvoCheckReturn3008 body = new InvoCheckReturn3008();
        body.setSerialNo("SN-IT-3");
        body.setInvoCode("123456789012");
        body.setInvoNum("87654321");
        body.setInvoAmt("not-a-number");
        body.setInvoDate("2026-13-99");
        body.setInvoCheckReturnCode("0");

        publisher.publishEvent(new InboundMessageProcessedEvent(
                MessageType.MSG_3008, "T0000003", "SN-IT-3", body, Instant.now()));

        final Optional<InvoiceVerificationRecordEntity> found =
                repository.findBySerialNo("SN-IT-3");
        assertThat(found).isPresent();
        assertThat(found.get().getInvoiceAmount()).isNull();
        assertThat(found.get().getInvoiceDate()).isNull();
        assertThat(found.get().getVerificationResult()).isEqualTo("0");
    }

    private static InvoCheckReturn3008 invoice(final String serialNo,
                                               final String returnCode,
                                               final String amount) {
        final InvoCheckReturn3008 b = new InvoCheckReturn3008();
        b.setSerialNo(serialNo);
        b.setInvoCode("123456789012");
        b.setInvoNum("87654321");
        b.setInvoAmt(amount);
        b.setInvoDate("20260601");
        b.setInvoCheckReturnCode(returnCode);
        return b;
    }
}
