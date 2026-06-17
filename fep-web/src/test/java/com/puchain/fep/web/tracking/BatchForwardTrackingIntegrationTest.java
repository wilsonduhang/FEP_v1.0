package com.puchain.fep.web.tracking;

import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.event.BatchForwardProcessedEvent;
import com.puchain.fep.web.integration.tracking.BatchForwardRecordEntity;
import com.puchain.fep.web.integration.tracking.BatchForwardRecordRepository;

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
 * End-to-end wiring for the batch forward tracking write path:
 * {@code ApplicationEventPublisher} → {@link com.puchain.fep.web.tracking.listener.BatchForwardEventListener}
 * → {@link com.puchain.fep.web.tracking.service.BatchForwardTrackingService}
 * → {@link BatchForwardRecordRepository} + Flyway {@code V41} (§6.4.1 FR-DATA-DB-01).
 *
 * <p><strong>Coverage boundary:</strong> this test publishes the
 * {@link BatchForwardProcessedEvent} directly, exercising the
 * listener→service→repo chain but not the {@code BatchMessageProcessorService}
 * terminal-state publish that emits it in production. The production trigger
 * (a real batch {@code CfxMessage}) is covered by the GHA strong-regression
 * suite; this boundary is documented intentionally.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@Transactional
@DisplayName("Batch forward tracking: event-published write path")
class BatchForwardTrackingIntegrationTest {

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private BatchForwardRecordRepository repository;

    @BeforeEach
    void cleanUp() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("publishing a batch forward event persists a mapped record")
    void publishEvent_persistsMappedRecord() {
        publisher.publishEvent(new BatchForwardProcessedEvent(
                MessageType.MSG_3009, "T0000001", 10, 8, 2,
                Instant.parse("2026-06-16T10:00:00Z"),
                Instant.parse("2026-06-16T10:00:05Z")));

        final Optional<BatchForwardRecordEntity> found = repository.findBySerialNo("T0000001");
        assertThat(found).isPresent();
        assertThat(found.get().getBatchType()).isEqualTo("3009");
        assertThat(found.get().getTotalRecordCount()).isEqualTo(10);
        assertThat(found.get().getSuccessRecordCount()).isEqualTo(8);
        assertThat(found.get().getBatchStatus()).isEqualTo("FAILED");
        assertThat(found.get().getErrorLogPath()).isNull();
        assertThat(found.get().getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("a repeated batch for the same serial updates idempotently (no duplicate)")
    void repeatedEvent_idempotentUpsert() {
        publisher.publishEvent(new BatchForwardProcessedEvent(
                MessageType.MSG_3009, "T0000002", 5, 5, 0,
                Instant.now(), Instant.now()));
        publisher.publishEvent(new BatchForwardProcessedEvent(
                MessageType.MSG_3009, "T0000002", 7, 6, 1,
                Instant.now(), Instant.now()));

        assertThat(repository.count()).isEqualTo(1L);
        assertThat(repository.findBySerialNo("T0000002"))
                .get()
                .extracting(BatchForwardRecordEntity::getBatchStatus,
                        BatchForwardRecordEntity::getTotalRecordCount)
                .containsExactly("FAILED", 7);
    }
}
