package com.puchain.fep.web.outbound;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.processor.intake.port.Direction;
import com.puchain.fep.processor.intake.port.EnqueueResult;
import com.puchain.fep.processor.intake.port.OutboundHeadFields;
import com.puchain.fep.processor.intake.port.OutboundMessageEnqueuePort;
import com.puchain.fep.processor.intake.port.OutboundMessageEnvelope;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JPA adapter behaviour for {@link OutboundMessageEnqueuePort} (P4 T7a).
 *
 * <p>Uses {@code @SpringBootTest} (not {@code @DataJpaTest}) because the H2
 * {@code MODE=MySQL} schema requires the full Flyway + application context, matching
 * the pattern set by {@link com.puchain.fep.web.integration.reconciliation.ReconciliationRecordRepositoryTest}.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@Transactional
@DisplayName("JpaOutboundMessageEnqueueService: enqueue + idempotency + marshal failure")
class JpaOutboundMessageEnqueueServiceTest {

    @Autowired
    private OutboundMessageEnqueuePort port;

    @Autowired
    private OutboundMessageQueueRepository repository;

    @BeforeEach
    void cleanUp() {
        repository.deleteAll();
    }

    @Test
    void firstSubmit_shouldReturnEnqueuedAndPersistRow() {
        final OutboundMessageEnvelope envelope = sampleEnvelope("k-first-001",
                new SamplePayload("INV-001", "12345.67"));

        final EnqueueResult result = port.submit(envelope);

        assertThat(result.status()).isEqualTo(EnqueueResult.Status.ENQUEUED);
        assertThat(result.queueId()).hasSize(32);

        final Optional<OutboundMessageQueueEntity> persisted =
                repository.findByIdempotencyKey("k-first-001");
        assertThat(persisted).isPresent();
        final OutboundMessageQueueEntity row = persisted.orElseThrow();
        assertThat(row.getQueueId()).isEqualTo(result.queueId());
        assertThat(row.getMessageType()).isEqualTo("3101");
        assertThat(row.getTransitionNo()).isEqualTo("00000001");
        assertThat(row.getStatus()).isEqualTo("PENDING");
        assertThat(row.getRetryCount()).isZero();
        assertThat(row.getErrorMessage()).isNull();
        assertThat(row.getNextRetryAt()).isNull();
        assertThat(row.getCreatedAt()).isNotNull();
        assertThat(row.getUpdatedAt()).isNotNull();
        assertThat(row.getPayloadDataType()).isEqualTo("INVOICE_CONTRACT_3101");
        assertThat(row.getSourceRef()).isEqualTo("ROW-1");
        // body XML should contain the marshalled payload element
        assertThat(row.getMessageBodyXml())
                .contains("<SamplePayload>")
                .contains("<invoiceNo>INV-001</invoiceNo>")
                .contains("<amount>12345.67</amount>");
        // head XML should be wrapped in <OutboundHead> with the 3 fields
        assertThat(row.getMessageHeadXml())
                .contains("<OutboundHead>")
                .contains("<SendOrgCode>A1000143000104</SendOrgCode>")
                .contains("<EntrustDate>20260501</EntrustDate>")
                .contains("<TransitionNo>00000001</TransitionNo>");
    }

    @Test
    void secondSubmit_sameIdempotencyKey_shouldThrowCollectDuplicateKey() {
        final OutboundMessageEnvelope first = sampleEnvelope("k-dup-001",
                new SamplePayload("INV-A", "10.00"));
        port.submit(first);

        final OutboundMessageEnvelope second = sampleEnvelope("k-dup-001",
                new SamplePayload("INV-A", "10.00"));

        assertThatThrownBy(() -> port.submit(second))
                .isInstanceOf(FepBusinessException.class)
                .extracting(ex -> ((FepBusinessException) ex).getErrorCode())
                .isEqualTo(FepErrorCode.COLLECT_DUPLICATE_KEY);

        // only one row persisted
        assertThat(repository.count()).isEqualTo(1L);
    }

    @Test
    void differentIdempotencyKeys_shouldEachEnqueue() {
        final EnqueueResult r1 = port.submit(sampleEnvelope("k-multi-001",
                new SamplePayload("INV-1", "1.00")));
        final EnqueueResult r2 = port.submit(sampleEnvelope("k-multi-002",
                new SamplePayload("INV-2", "2.00")));

        assertThat(r1.status()).isEqualTo(EnqueueResult.Status.ENQUEUED);
        assertThat(r2.status()).isEqualTo(EnqueueResult.Status.ENQUEUED);
        assertThat(r1.queueId()).isNotEqualTo(r2.queueId());
        assertThat(repository.count()).isEqualTo(2L);
    }

    @Test
    void nonJaxbMessageBody_shouldThrowCollectPersistFailure() {
        // Object.class has no JAXB annotations → JAXBContext build will reject it
        // (JaxbContextCache wraps JAXBException as IllegalStateException, which the
        // adapter must catch and rewrap as COLLECT_PERSIST_FAILURE).
        final OutboundMessageEnvelope envelope = sampleEnvelope("k-bad-001", new Object());

        assertThatThrownBy(() -> port.submit(envelope))
                .isInstanceOf(FepBusinessException.class)
                .extracting(ex -> ((FepBusinessException) ex).getErrorCode())
                .isEqualTo(FepErrorCode.COLLECT_PERSIST_FAILURE);

        assertThat(repository.count()).isZero();
    }

    private static OutboundMessageEnvelope sampleEnvelope(final String idempotencyKey,
                                                          final Object body) {
        return new OutboundMessageEnvelope(
                "3101",
                Direction.OUTBOUND,
                idempotencyKey,
                new OutboundHeadFields("A1000143000104", "20260501", "00000001"),
                body,
                "INVOICE_CONTRACT_3101",
                "ROW-1");
    }

    /**
     * Minimal JAXB-annotated payload used as a stand-in for production CFX bodies.
     */
    @XmlRootElement(name = "SamplePayload")
    public static class SamplePayload {
        private String invoiceNo;
        private String amount;

        public SamplePayload() { }

        public SamplePayload(final String invoiceNo, final String amount) {
            this.invoiceNo = invoiceNo;
            this.amount = amount;
        }

        @XmlElement
        public String getInvoiceNo() { return invoiceNo; }
        public void setInvoiceNo(final String invoiceNo) { this.invoiceNo = invoiceNo; }

        @XmlElement
        public String getAmount() { return amount; }
        public void setAmount(final String amount) { this.amount = amount; }
    }
}
