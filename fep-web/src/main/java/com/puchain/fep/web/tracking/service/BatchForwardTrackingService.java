package com.puchain.fep.web.tracking.service;

import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.processor.event.BatchForwardProcessedEvent;
import com.puchain.fep.web.integration.tracking.BatchForwardRecordEntity;
import com.puchain.fep.web.integration.tracking.BatchForwardRecordRepository;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;

/**
 * Maps a {@link BatchForwardProcessedEvent} (batch pipeline terminal state) into a
 * {@link BatchForwardRecordEntity} and persists it idempotently
 * (§6.4.1 FR-DATA-DB-01, PRD v1.3 §2020 非实时业务转发记录表).
 *
 * <p>Idempotency: keyed by the batch {@code transitionNo} (unique constraint
 * {@code uq_batch_forward_serial}). When the batch {@code transitionNo} is a stable
 * {@code head.msgId}, a re-delivered batch updates the existing row; when it is a
 * generated placeholder, every run inserts a fresh row — neither violates the unique
 * constraint (see Plan §1.1 idempotency note).</p>
 *
 * <p>{@code batchType} stores the raw message {@code msgNo} and {@code batchStatus}
 * the raw {@code COMPLETED}/{@code FAILED} state name; semantic ENUM mapping is
 * DEFERRED to the domain expert (see {@code DEF-B2-2}). {@code errorLogPath} stays
 * {@code null} (FEP does not persist batch error-log files — zero fabrication).</p>
 *
 * <p>Own {@code @Transactional} boundary (DECISION-4): the batch pipeline publishes
 * the event from {@code fep-processor} without a surrounding web transaction, so this
 * service opens its own so the side-table write is atomic and independent.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Service
@SuppressFBWarnings(value = "CRLF_INJECTION_LOGS",
        justification = "serialNo passed through LogSanitizer.sanitize() prior to LOG; "
                + "batchType is an enum-derived msgNo (not user input)")
public class BatchForwardTrackingService {

    private static final Logger LOG =
            LoggerFactory.getLogger(BatchForwardTrackingService.class);

    /** PK column length (VARCHAR(32)); business transitionNo is bounded below this. */
    private static final int BATCH_FORWARD_ID_MAX_LEN = 32;

    /** State name stored in {@code batch_status} when the batch fully succeeded. */
    private static final String STATUS_COMPLETED = "COMPLETED";

    /** State name stored in {@code batch_status} when at least one record failed. */
    private static final String STATUS_FAILED = "FAILED";

    private final BatchForwardRecordRepository repository;

    /**
     * Spring constructor injection.
     *
     * @param repository batch forward record repository, non-null
     */
    public BatchForwardTrackingService(final BatchForwardRecordRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    /**
     * Persists (insert or idempotent update) the batch forward tracking record
     * derived from a {@link BatchForwardProcessedEvent}.
     *
     * @param event the batch forward processed event, non-null
     */
    @Transactional
    public void track(final BatchForwardProcessedEvent event) {
        Objects.requireNonNull(event, "event");
        final String serialNo = event.transitionNo();

        final BatchForwardRecordEntity entity = repository.findBySerialNo(serialNo)
                .orElseGet(BatchForwardRecordEntity::new);
        if (entity.getBatchForwardId() == null) {
            entity.setBatchForwardId(deriveId(serialNo));
        }
        entity.setSerialNo(serialNo);
        entity.setBatchType(event.type().msgNo());
        entity.setTotalRecordCount(event.total());
        entity.setSuccessRecordCount(event.success());
        entity.setProcessStartTime(toLocalDateTime(event.startedAt()));
        entity.setProcessEndTime(toLocalDateTime(event.finishedAt()));
        entity.setBatchStatus(event.failed() == 0 ? STATUS_COMPLETED : STATUS_FAILED);
        entity.setErrorLogPath(null);

        repository.save(entity);
        LOG.info("batch forward tracked serialNo={} type={} total={} success={} failed={}",
                LogSanitizer.sanitize(serialNo), event.type().msgNo(),
                event.total(), event.success(), event.failed());
    }

    private static String deriveId(final String serialNo) {
        return serialNo.length() <= BATCH_FORWARD_ID_MAX_LEN
                ? serialNo
                : serialNo.substring(0, BATCH_FORWARD_ID_MAX_LEN);
    }

    private static LocalDateTime toLocalDateTime(final java.time.Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }
}
