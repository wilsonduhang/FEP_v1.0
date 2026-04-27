package com.puchain.fep.web.integration.reconciliation;

import com.puchain.fep.processor.reconciliation.ClearingInstructionRecord;
import com.puchain.fep.processor.reconciliation.ClearingInstructionStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end test for {@link JpaClearingInstructionStore} adapter.
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@Transactional
@DisplayName("JpaClearingInstructionStore: adapter round-trip")
class JpaClearingInstructionStoreTest {

    @Autowired
    private ClearingInstructionStore store;

    @Autowired
    private ClearingInstructionRecordRepository repository;

    @BeforeEach
    void cleanUp() {
        repository.deleteAll();
    }

    @Test
    void primaryAdapter_shouldBeJpaImplementation() {
        assertThat(store).isInstanceOf(JpaClearingInstructionStore.class);
    }

    @Test
    void save_shouldRoundTripCompositeKeyAndAllFields() {
        final LocalDateTime now = LocalDateTime.of(2026, 4, 27, 12, 0);
        final ClearingInstructionRecord record = ClearingInstructionRecord.builder()
                .instructionId("I-AD1")
                .qsSerialNo("QS-AD1")
                .instructionType("ERROR_HANDLING")
                .settlementAmount(new BigDecimal("9876.5432"))
                .payerAccount("6225880100000001")
                .payeeAccount("6225880100000002")
                .instructionStatus("PROCESSING")
                .messageId("MSG-AD1")
                .createdAt(now)
                .updatedAt(now)
                .build();

        final ClearingInstructionRecord saved = store.save(record);

        assertThat(saved.getInstructionId()).isEqualTo("I-AD1");
        assertThat(saved.getQsSerialNo()).isEqualTo("QS-AD1");
        assertThat(saved.getInstructionType()).isEqualTo("ERROR_HANDLING");
        assertThat(saved.getSettlementAmount()).isEqualByComparingTo(new BigDecimal("9876.5432"));
        assertThat(saved.getInstructionStatus()).isEqualTo("PROCESSING");
        assertThat(saved.getExecutionTime()).isNull();
        assertThat(saved.getFailureCause()).isNull();
    }

    @Test
    void findByMessageId_andFindByStatus_shouldDelegateCorrectly() {
        final LocalDateTime now = LocalDateTime.of(2026, 4, 27, 12, 0);
        store.save(builder("I-AD2", "QS-AD2", "MSG-AD2", "PENDING", now));
        store.save(builder("I-AD3", "QS-AD3", "MSG-AD2", "FAILED", now));
        store.save(builder("I-AD4", "QS-AD4", "MSG-AD3", "PENDING", now));

        final List<ClearingInstructionRecord> linked = store.findByMessageId("MSG-AD2");
        assertThat(linked).hasSize(2)
                .extracting(ClearingInstructionRecord::getInstructionId)
                .containsExactlyInAnyOrder("I-AD2", "I-AD3");

        final Optional<ClearingInstructionRecord> single =
                store.findByInstructionIdAndQsSerialNo("I-AD3", "QS-AD3");
        assertThat(single).map(ClearingInstructionRecord::getInstructionStatus).contains("FAILED");

        assertThat(store.findByStatus("PENDING")).hasSize(2);
    }

    private static ClearingInstructionRecord builder(final String instructionId,
                                                     final String qsSerialNo,
                                                     final String messageId,
                                                     final String status,
                                                     final LocalDateTime now) {
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
