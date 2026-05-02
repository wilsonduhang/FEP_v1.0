package com.puchain.fep.web.collector;

import com.puchain.fep.collector.run.CollectionRunRecorder;
import com.puchain.fep.collector.support.CollectionRunResult;
import com.puchain.fep.collector.support.TriggerType;
import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link JdbcCollectionRunRecorderService} behaviour test (P4 T8).
 *
 * <p>Uses {@code @SpringBootTest} so the recorder is wired against the real
 * H2 schema applied by Flyway V23, mirroring the T7a precedent
 * ({@link com.puchain.fep.web.outbound.JpaOutboundMessageEnqueueServiceTest}).</p>
 *
 * <p>Coverage:
 * <ul>
 *   <li>Happy path — {@code start} writes RUNNING row, {@code complete} updates to terminal status</li>
 *   <li>Error path — {@code complete} on missing run_id raises {@code COLLECT_PERSIST_FAILURE}</li>
 *   <li>DataAccessException on save() during start() rewraps to {@code COLLECT_PERSIST_FAILURE}</li>
 *   <li>Status enum names round-trip through the VARCHAR(16) column (boundary)</li>
 * </ul>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@DisplayName("JdbcCollectionRunRecorderService: start RUNNING + complete terminal + missing-row + DAE rewrap")
class JdbcCollectionRunRecorderServiceTest {

    private static final String RUN_ID = "RUN000000000000000000000000T8001";
    private static final String ADAPTER_ID = "ADP_T8";

    @Autowired
    private CollectionRunRecorder recorder;

    @Autowired
    private CollectionRunRepository repository;

    @BeforeEach
    void purgeBefore() {
        repository.deleteAll();
    }

    @AfterEach
    void purgeAfter() {
        repository.deleteAll();
    }

    @Test
    void start_thenComplete_shouldWriteRunningRowAndUpdateToTerminalStatus() {
        final Instant startedAt = Instant.parse("2026-04-30T10:00:00Z");
        final Instant completedAt = startedAt.plus(3, ChronoUnit.SECONDS);

        recorder.start(RUN_ID, ADAPTER_ID, TriggerType.MANUAL, startedAt);
        final CollectionRunEntity afterStart = repository.findById(RUN_ID).orElseThrow();
        assertThat(afterStart.getStatus()).isEqualTo("RUNNING");
        assertThat(afterStart.getAdapterId()).isEqualTo(ADAPTER_ID);
        assertThat(afterStart.getTriggerSource()).isEqualTo("MANUAL");
        assertThat(afterStart.getStartedAt()).isEqualTo(startedAt);
        assertThat(afterStart.getCompletedAt()).isNull();
        assertThat(afterStart.getCollectedCount()).isZero();
        assertThat(afterStart.getAssembledCount()).isZero();
        assertThat(afterStart.getSubmittedCount()).isZero();
        assertThat(afterStart.getErrorCount()).isZero();
        assertThat(afterStart.getErrorMessage()).isNull();
        assertThat(afterStart.getCreatedAt()).isEqualTo(startedAt);

        recorder.complete(RUN_ID, CollectionRunResult.Status.PARTIAL, 5, 4, 1,
                "first failure: missing field", completedAt);
        final CollectionRunEntity afterComplete = repository.findById(RUN_ID).orElseThrow();
        assertThat(afterComplete.getStatus()).isEqualTo("PARTIAL");
        assertThat(afterComplete.getAssembledCount()).isEqualTo(5);
        assertThat(afterComplete.getSubmittedCount()).isEqualTo(4);
        assertThat(afterComplete.getErrorCount()).isEqualTo(1);
        assertThat(afterComplete.getErrorMessage()).isEqualTo("first failure: missing field");
        assertThat(afterComplete.getCompletedAt()).isEqualTo(completedAt);
        // Started_at must remain untouched on complete
        assertThat(afterComplete.getStartedAt()).isEqualTo(startedAt);
    }

    @Test
    void complete_withMissingRunId_shouldThrowCollectPersistFailure() {
        // No prior start() call → row absent.
        assertThatThrownBy(() -> recorder.complete("RUN_MISSING_____________________",
                CollectionRunResult.Status.SUCCESS, 0, 0, 0, null,
                Instant.parse("2026-04-30T10:00:00Z")))
                .isInstanceOf(FepBusinessException.class)
                .extracting(ex -> ((FepBusinessException) ex).getErrorCode())
                .isEqualTo(FepErrorCode.COLLECT_PERSIST_FAILURE);
    }

    @Test
    void start_whenRepositorySaveThrowsDataAccess_shouldRewrapToCollectPersistFailure() {
        // Mock-driven path: a real save failure is hard to provoke in H2; mocking
        // the repository directly proves the catch block surfaces the canonical
        // FepErrorCode.COLLECT_PERSIST_FAILURE without leaking the JDBC exception.
        final CollectionRunRepository mockRepo = mock(CollectionRunRepository.class);
        when(mockRepo.save(any(CollectionRunEntity.class)))
                .thenThrow(new DataIntegrityViolationException("simulated UNIQUE conflict"));

        final JdbcCollectionRunRecorderService mockRecorder = new JdbcCollectionRunRecorderService(mockRepo);

        assertThatThrownBy(() -> mockRecorder.start(RUN_ID, ADAPTER_ID, TriggerType.SCHEDULED,
                Instant.parse("2026-04-30T11:00:00Z")))
                .isInstanceOf(FepBusinessException.class)
                .satisfies(ex -> {
                    final FepBusinessException fbe = (FepBusinessException) ex;
                    assertThat(fbe.getErrorCode()).isEqualTo(FepErrorCode.COLLECT_PERSIST_FAILURE);
                    assertThat(fbe.getCause()).isInstanceOf(DataAccessException.class);
                });
    }

    @Test
    void complete_shouldAcceptAllTerminalStatusEnumNames() {
        // VARCHAR(16) must accept SUCCESS / PARTIAL / FAILED / SKIPPED — all <= 16 chars.
        for (CollectionRunResult.Status terminal : new CollectionRunResult.Status[]{
                CollectionRunResult.Status.SUCCESS,
                CollectionRunResult.Status.PARTIAL,
                CollectionRunResult.Status.FAILED,
                CollectionRunResult.Status.SKIPPED}) {
            // SKIPPED is unusual to write here (scheduler short-circuits before recorder.start)
            // but we still verify it round-trips so future code paths aren't blocked.
            final String runId = ("R000000000000000000000000000" + terminal.name()).substring(0, 32);
            final Instant t = Instant.parse("2026-04-30T12:00:00Z");
            recorder.start(runId, ADAPTER_ID, TriggerType.SCHEDULED, t);
            recorder.complete(runId, terminal, 1, 1, 0, null, t.plusSeconds(1));
            assertThat(repository.findById(runId).orElseThrow().getStatus())
                    .isEqualTo(terminal.name());
        }
    }
}
