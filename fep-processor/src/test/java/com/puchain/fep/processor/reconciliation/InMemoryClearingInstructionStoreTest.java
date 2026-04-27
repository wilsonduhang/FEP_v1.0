package com.puchain.fep.processor.reconciliation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CRUD + finder behaviour for {@link InMemoryClearingInstructionStore}.
 *
 * @author FEP Team
 * @since 1.0.0
 */
@DisplayName("InMemoryClearingInstructionStore: CRUD + finder")
class InMemoryClearingInstructionStoreTest {

    private InMemoryClearingInstructionStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryClearingInstructionStore();
    }

    @Test
    void save_shouldPersistByCompositeKey() {
        final ClearingInstructionRecord record = sampleRecord("I1", "QS1", "MSG1", "PENDING");

        final ClearingInstructionRecord saved = store.save(record);

        assertThat(saved).isSameAs(record);
        assertThat(store.findByInstructionIdAndQsSerialNo("I1", "QS1"))
                .containsSame(record);
    }

    @Test
    void findByInstructionIdAndQsSerialNo_shouldReturnEmpty_whenNotFound() {
        store.save(sampleRecord("I1", "QS1", "MSG1", "PENDING"));

        final Optional<ClearingInstructionRecord> result =
                store.findByInstructionIdAndQsSerialNo("I1", "MISSING");

        assertThat(result).isEmpty();
    }

    @Test
    void findByMessageId_shouldReturnAllLinkedRecords() {
        store.save(sampleRecord("I1", "QS1", "MSG1", "PENDING"));
        store.save(sampleRecord("I2", "QS2", "MSG1", "PROCESSING"));
        store.save(sampleRecord("I3", "QS3", "MSG2", "PENDING"));

        final List<ClearingInstructionRecord> linked = store.findByMessageId("MSG1");

        assertThat(linked).hasSize(2)
                .extracting(ClearingInstructionRecord::getInstructionId)
                .containsExactlyInAnyOrder("I1", "I2");
    }

    @Test
    void findByStatus_shouldFilterCorrectly() {
        store.save(sampleRecord("I1", "QS1", "MSG1", "PENDING"));
        store.save(sampleRecord("I2", "QS2", "MSG1", "FAILED"));
        store.save(sampleRecord("I3", "QS3", "MSG2", "PENDING"));

        final List<ClearingInstructionRecord> pending = store.findByStatus("PENDING");
        final List<ClearingInstructionRecord> failed = store.findByStatus("FAILED");

        assertThat(pending).hasSize(2);
        assertThat(failed).hasSize(1)
                .extracting(ClearingInstructionRecord::getInstructionId)
                .containsExactly("I2");
    }

    @Test
    void save_shouldOverwriteByCompositeKey() {
        store.save(sampleRecord("I1", "QS1", "MSG1", "PENDING"));

        final ClearingInstructionRecord existing = store.findByInstructionIdAndQsSerialNo("I1", "QS1")
                .orElseThrow();
        final ClearingInstructionRecord updated = ClearingInstructionRecord.builder()
                .from(existing)
                .instructionStatus("SUCCESS")
                .executionTime(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        store.save(updated);

        assertThat(store.findByInstructionIdAndQsSerialNo("I1", "QS1"))
                .map(ClearingInstructionRecord::getInstructionStatus)
                .contains("SUCCESS");
    }

    private static ClearingInstructionRecord sampleRecord(final String instructionId,
                                                          final String qsSerialNo,
                                                          final String messageId,
                                                          final String status) {
        final LocalDateTime now = LocalDateTime.of(2026, 4, 27, 12, 0);
        return ClearingInstructionRecord.builder()
                .instructionId(instructionId)
                .qsSerialNo(qsSerialNo)
                .instructionType("NORMAL")
                .settlementAmount(new BigDecimal("100.0000"))
                .payerAccount("6225880100000001")
                .payeeAccount("6225880100000002")
                .instructionStatus(status)
                .messageId(messageId)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
