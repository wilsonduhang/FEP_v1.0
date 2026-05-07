package com.puchain.fep.web.outbound;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.common.util.FepConstants;
import com.puchain.fep.processor.intake.port.Direction;
import com.puchain.fep.processor.intake.port.EnqueueResult;
import com.puchain.fep.processor.intake.port.OutboundHeadFields;
import com.puchain.fep.processor.intake.port.OutboundMessageEnqueuePort;
import com.puchain.fep.processor.intake.port.OutboundMessageEnvelope;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * JPA adapter behaviour for {@link OutboundMessageEnqueuePort} (P4 T7a).
 *
 * <p>Uses {@code @SpringBootTest} (not {@code @DataJpaTest}) because the H2
 * {@code MODE=MySQL} schema requires the full Flyway + application context, matching
 * the pattern set by {@link com.puchain.fep.web.integration.reconciliation.ReconciliationRecordRepositoryTest}.
 * The original Plan §T7a #7 wording referenced {@code @DataJpaTest + @Import}, but
 * the production schema profile is incompatible.</p>
 *
 * <p><b>Test isolation (T7a-fix M1):</b> The Service uses
 * {@link org.springframework.transaction.annotation.Propagation#REQUIRES_NEW}, so each
 * {@code submit()} commits the row independently of any caller transaction. The test
 * class therefore does NOT carry a {@code @Transactional} annotation — instead, every
 * test ends with an {@code @AfterEach} {@code repository.deleteAll()} that runs in its
 * own (Spring Data default) transaction and physically purges the table between tests.
 * This avoids cross-test row accumulation that would otherwise be invisible inside the
 * test's own transactional view.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@DisplayName("JpaOutboundMessageEnqueueService: enqueue + idempotency + marshal failure + boundaries + race-path")
class JpaOutboundMessageEnqueueServiceTest {

    @Autowired
    private OutboundMessageEnqueuePort port;

    @Autowired
    private OutboundMessageQueueRepository repository;

    @BeforeEach
    void cleanUpBefore() {
        repository.deleteAll();
    }

    @AfterEach
    void cleanUpAfter() {
        // T7a-fix M1: purge physical rows committed by the Service's REQUIRES_NEW
        // transactions so the next test starts from an empty table.
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
                .contains("<SendOrgCode>" + FepConstants.HNDEMP_NODE_CODE + "</SendOrgCode>")
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

    /** T7a-fix M2: idempotencyKey at the column boundary (V22 VARCHAR(64)). */
    @Test
    void submit_with64CharIdempotencyKey_shouldEnqueue() {
        final String boundaryKey = "a".repeat(64);
        final OutboundMessageEnvelope envelope = sampleEnvelope(boundaryKey,
                new SamplePayload("INV-B", "0.01"));

        final EnqueueResult result = port.submit(envelope);

        assertThat(result.status()).isEqualTo(EnqueueResult.Status.ENQUEUED);
        final Optional<OutboundMessageQueueEntity> persisted =
                repository.findByIdempotencyKey(boundaryKey);
        assertThat(persisted).isPresent();
        assertThat(persisted.orElseThrow().getIdempotencyKey()).hasSize(64);
    }

    /** T7a-fix M2: very long body XML must marshal + persist intact (TEXT column). */
    @Test
    void submit_withVeryLongBodyXml_shouldPersist() {
        final List<String> manyItems = new ArrayList<>(200);
        for (int i = 0; i < 200; i++) {
            manyItems.add("item-" + i + "-payload-segment-with-some-bytes");
        }
        final LargeSamplePayload large = new LargeSamplePayload();
        large.setItems(manyItems);

        final OutboundMessageEnvelope envelope = sampleEnvelope("k-long-001", large);
        final EnqueueResult result = port.submit(envelope);

        assertThat(result.status()).isEqualTo(EnqueueResult.Status.ENQUEUED);
        final OutboundMessageQueueEntity row =
                repository.findByIdempotencyKey("k-long-001").orElseThrow();
        // Sanity: body XML must contain first and last items + the wrapper element
        assertThat(row.getMessageBodyXml())
                .contains("<LargeSamplePayload>")
                .contains("<item>item-0-payload-segment-with-some-bytes</item>")
                .contains("<item>item-199-payload-segment-with-some-bytes</item>");
        // Size sanity: at least 6 KB of XML for 200 items
        assertThat(row.getMessageBodyXml().length()).isGreaterThan(6_000);
    }

    /**
     * T7a-fix M3: race-window catch path coverage.
     *
     * <p>The pre-flight {@code existsByIdempotencyKey} guard always wins in
     * sequential tests; the {@code DataIntegrityViolationException} catch at
     * {@code JpaOutboundMessageEnqueueService.java:73-79} only fires when a concurrent
     * insert wins between the pre-flight read and the {@code save}. Reproducing this
     * with two real threads is fragile; instead we instantiate the Service directly
     * with a Mockito-mocked repository that simulates the race outcome (pre-flight
     * returns false, save throws DIVE).</p>
     */
    @Test
    void saveThrowsDataIntegrityViolation_shouldMapToCollectDuplicateKey() {
        final OutboundMessageQueueRepository mockRepo =
                mock(OutboundMessageQueueRepository.class);
        when(mockRepo.existsByIdempotencyKey(anyString())).thenReturn(false);
        when(mockRepo.save(any(OutboundMessageQueueEntity.class)))
                .thenThrow(new DataIntegrityViolationException("simulated UNIQUE conflict"));

        final JpaOutboundMessageEnqueueService raceService =
                new JpaOutboundMessageEnqueueService(mockRepo);
        final OutboundMessageEnvelope env = sampleEnvelope("RACE_KEY",
                new SamplePayload("INV-RACE", "0.99"));

        assertThatThrownBy(() -> raceService.submit(env))
                .isInstanceOf(FepBusinessException.class)
                .satisfies(ex -> {
                    final FepBusinessException fbe = (FepBusinessException) ex;
                    assertThat(fbe.getErrorCode()).isEqualTo(FepErrorCode.COLLECT_DUPLICATE_KEY);
                    assertThat(fbe.getCause())
                            .isInstanceOf(DataIntegrityViolationException.class);
                });
    }

    private static OutboundMessageEnvelope sampleEnvelope(final String idempotencyKey,
                                                          final Object body) {
        return new OutboundMessageEnvelope(
                "3101",
                Direction.OUTBOUND,
                idempotencyKey,
                new OutboundHeadFields(FepConstants.HNDEMP_NODE_CODE, "20260501", "00000001"),
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

    /**
     * JAXB payload with a list field that produces large XML for boundary testing.
     */
    @XmlRootElement(name = "LargeSamplePayload")
    public static class LargeSamplePayload {
        private List<String> items = new ArrayList<>();

        public LargeSamplePayload() { }

        @XmlElementWrapper(name = "items")
        @XmlElement(name = "item")
        public List<String> getItems() { return items; }
        public void setItems(final List<String> items) { this.items = items; }
    }
}
