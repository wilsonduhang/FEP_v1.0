package com.puchain.fep.web.collector;

import com.puchain.fep.collector.run.CollectionRunRecorder;
import com.puchain.fep.collector.support.CollectionRunResult;
import com.puchain.fep.collector.support.TriggerType;
import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.common.util.LogSanitizer;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * fep-web Adapter — JPA implementation of {@link CollectionRunRecorder}
 * (P4 T8, FR-MSG-MODE-DW-PERSIST).
 *
 * <p>Persists the lifecycle events of {@code CollectorScheduler.runAdapter}
 * into the {@code collection_run} table via {@link CollectionRunRepository}.
 * Two transitions are written per run:
 * <ol>
 *   <li>{@link #start} — INSERT a {@code RUNNING} row when the scheduler takes
 *       the distributed lock. Persistence failure here aborts the run (the
 *       scheduler's HIGH#1 path catches it and synthesizes a FAILED result while
 *       releasing the lock — see {@code CollectorScheduler#runAdapter}).</li>
 *   <li>{@link #complete} — UPDATE the row with the terminal status and counts
 *       in the {@code finally} block of the orchestration. Persistence failure
 *       here is suppressed by the scheduler's MEDIUM#3 {@code safeRecorderComplete}
 *       wrapper to preserve the original orchestration result.</li>
 * </ol>
 *
 * <p><b>Transaction:</b> {@link Propagation#REQUIRES_NEW} mirrors the T7a
 * precedent ({@code JpaOutboundMessageEnqueueService}) so the recorder commits
 * independently of any outer transaction; the run-state row stays auditable
 * even if a downstream caller transaction rolls back.</p>
 *
 * <p><b>Adapter package placement (Option A):</b> placed in
 * {@code com.puchain.fep.web.collector} to be discovered by
 * {@code @EnableJpaRepositories(basePackages = "com.puchain.fep.web")} in
 * {@code JpaConfiguration}. See {@link CollectionRunEntity} class javadoc for
 * the full deviation rationale vs Plan §T8 §2.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Service
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class JdbcCollectionRunRecorderService implements CollectionRunRecorder {

    /**
     * Initial status assigned at {@link #start} time. Mirrors the
     * {@code CollectionRunResult.Status} enum names but is intentionally a string
     * literal to avoid coupling fep-web to fep-collector's enum class for this
     * ephemeral transition value.
     */
    private static final String STATUS_RUNNING = "RUNNING";

    private final CollectionRunRepository repository;

    /**
     * Spring constructor injection.
     *
     * @param repository non-null Spring Data repository
     */
    public JdbcCollectionRunRecorderService(final CollectionRunRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    @Override
    public void start(final String runId,
                      final String adapterId,
                      final TriggerType triggerType,
                      final Instant startedAt) {
        Objects.requireNonNull(runId, "runId");
        Objects.requireNonNull(adapterId, "adapterId");
        Objects.requireNonNull(triggerType, "triggerType");
        Objects.requireNonNull(startedAt, "startedAt");

        final CollectionRunEntity entity = new CollectionRunEntity();
        entity.setRunId(runId);
        entity.setAdapterId(adapterId);
        entity.setStatus(STATUS_RUNNING);
        entity.setStartedAt(startedAt);
        entity.setCompletedAt(null);
        entity.setCollectedCount(0);
        entity.setAssembledCount(0);
        entity.setSubmittedCount(0);
        entity.setErrorCount(0);
        entity.setErrorMessage(null);
        entity.setTriggerSource(triggerType.name());
        entity.setCreatedAt(startedAt);
        try {
            repository.save(entity);
        } catch (DataAccessException dae) {
            throw new FepBusinessException(
                    FepErrorCode.COLLECT_PERSIST_FAILURE,
                    "failed to persist RUNNING collection_run row for adapterId="
                            + LogSanitizer.sanitize(adapterId),
                    dae);
        }
    }

    @Override
    public void complete(final String runId,
                         final CollectionRunResult.Status status,
                         final int collected,
                         final int assembled,
                         final int submitted,
                         final int errors,
                         final String errorMessage,
                         final Instant completedAt) {
        Objects.requireNonNull(runId, "runId");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(completedAt, "completedAt");

        final Optional<CollectionRunEntity> found = repository.findById(runId);
        if (found.isEmpty()) {
            // No RUNNING row to update — the scheduler's safeRecorderComplete
            // wrapper handles this case as a "nothing to do" (runId == null path).
            // We still raise so the wrapper can log a clear suppression reason.
            throw new FepBusinessException(
                    FepErrorCode.COLLECT_PERSIST_FAILURE,
                    "collection_run row not found for runId=" + LogSanitizer.sanitize(runId));
        }
        // .get() is safe here — the isEmpty() guard above already throws on absent;
        // .orElseThrow() reads as a redundant double-throw (T8-fix MEDIUM #1).
        final CollectionRunEntity entity = found.get();
        entity.setStatus(status.name());
        // T10 Simplify Q-2 fix: persist the raw adapter.collect() count so the
        // management UI can distinguish total-collected from assembled/submitted.
        entity.setCollectedCount(collected);
        entity.setAssembledCount(assembled);
        entity.setSubmittedCount(submitted);
        entity.setErrorCount(errors);
        entity.setErrorMessage(errorMessage);
        entity.setCompletedAt(completedAt);
        try {
            repository.save(entity);
        } catch (DataAccessException dae) {
            throw new FepBusinessException(
                    FepErrorCode.COLLECT_PERSIST_FAILURE,
                    "failed to persist terminal collection_run row for runId="
                            + LogSanitizer.sanitize(runId),
                    dae);
        }
    }
}
