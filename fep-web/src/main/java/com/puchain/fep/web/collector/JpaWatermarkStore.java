package com.puchain.fep.web.collector;

import com.puchain.fep.collector.support.WatermarkStore;
import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.common.util.LogSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * fep-web Adapter — JPA implementation of {@link WatermarkStore} (P4 T8-fix).
 *
 * <p>Persists adapter cursors in the {@code collection_record_offset} table
 * created by Flyway migration V23. Replaces the previous
 * {@code JdbcWatermarkStore} which used H2-proprietary {@code MERGE INTO ... KEY(col)}
 * syntax that does not parse on MySQL 8.0+ — Hibernate now emits dialect-correct
 * SQL for both H2 (MODE=MySQL) and MySQL 8 prod transparently.</p>
 *
 * <p><b>Activation:</b> {@code @Profile("!test")} means this bean is created in
 * dev / prod profiles but NOT under the {@code test} profile, where unit tests
 * continue to instantiate {@code InMemoryWatermarkStore} directly. Spring picks
 * up the {@link CollectionRecordOffsetRepository} bean auto-configured by
 * Spring Data JPA.</p>
 *
 * <p><b>UPSERT semantics:</b> Spring Data {@code save(entity)} performs an INSERT
 * if no row exists for the {@code @Id} (adapterId) and an UPDATE otherwise. This
 * mirrors the prior MERGE behavior, but is implemented at the Hibernate level so
 * the underlying SQL is generated for the active dialect — no vendor-specific SQL
 * lives in the codebase.</p>
 *
 * <p><b>Adapter package placement:</b> located in {@code com.puchain.fep.web.collector}
 * to satisfy {@code @EnableJpaRepositories(basePackages = "com.puchain.fep.web")}
 * declared in {@code JpaConfiguration}, while the SPI port
 * ({@link WatermarkStore}) remains in fep-collector — mirroring the T7a precedent
 * (Port in inner module, Adapter implementation in fep-web).</p>
 *
 * <p><b>Transaction:</b> {@link Propagation#REQUIRES_NEW} mirrors the T7a
 * precedent ({@code JpaOutboundMessageEnqueueService}) so the watermark commits
 * independently of any outer transaction; the cursor stays advanced even if a
 * downstream caller transaction rolls back.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
@Profile("!test")
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class JpaWatermarkStore implements WatermarkStore {

    private static final Logger LOG = LoggerFactory.getLogger(JpaWatermarkStore.class);

    private final CollectionRecordOffsetRepository repository;

    /**
     * Spring constructor injection.
     *
     * @param repository non-null Spring Data JPA repository
     */
    public JpaWatermarkStore(final CollectionRecordOffsetRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Optional<String> get(final String adapterId) {
        Objects.requireNonNull(adapterId, "adapterId");
        try {
            return repository.findById(adapterId)
                    .map(CollectionRecordOffsetEntity::getWatermark);
        } catch (DataAccessException dae) {
            LOG.error("watermark read failed for adapterId={}",
                    LogSanitizer.sanitize(adapterId), dae);
            throw new FepBusinessException(
                    FepErrorCode.COLLECT_PERSIST_FAILURE,
                    "failed to read watermark for adapterId=" + LogSanitizer.sanitize(adapterId),
                    dae);
        }
    }

    @Override
    public void put(final String adapterId, final String watermark) {
        Objects.requireNonNull(adapterId, "adapterId");
        Objects.requireNonNull(watermark, "watermark");
        // T10 Simplify Q-3 fix: blank watermark would cause silent data replay on next
        // collect (WHERE cursor_col > '' matches every row). Reject at the persistence
        // boundary to protect ALL adapters (not just JDBC), fail-fast over corrupting
        // collection_record_offset.
        if (watermark.isBlank()) {
            throw new IllegalArgumentException(
                    "watermark must not be blank (would cause cursor regression to start);"
                            + " adapterId=" + adapterId);
        }
        try {
            final CollectionRecordOffsetEntity entity = new CollectionRecordOffsetEntity();
            entity.setAdapterId(adapterId);
            entity.setWatermark(watermark);
            entity.setUpdatedAt(Instant.now());
            repository.save(entity);
        } catch (DataAccessException dae) {
            LOG.error("watermark write failed for adapterId={}",
                    LogSanitizer.sanitize(adapterId), dae);
            throw new FepBusinessException(
                    FepErrorCode.COLLECT_PERSIST_FAILURE,
                    "failed to write watermark for adapterId=" + LogSanitizer.sanitize(adapterId),
                    dae);
        }
    }
}
