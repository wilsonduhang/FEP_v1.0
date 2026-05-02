package com.puchain.fep.collector.support;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.common.util.LogSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * JDBC-backed {@link WatermarkStore} that persists adapter cursors in the
 * {@code collection_record_offset} table created by V23 (P4 T8).
 *
 * <p><b>Activation:</b> {@code @Profile("!test")} means this bean is created in
 * dev / prod profiles but NOT under the {@code test} profile, where unit tests
 * continue to instantiate {@link InMemoryWatermarkStore} directly. Spring picks
 * up the {@link JdbcTemplate} bean auto-configured by Spring Boot when fep-collector
 * runs as part of the fep-web application context.</p>
 *
 * <p><b>UPSERT semantics:</b> the {@link #put} call uses {@code MERGE INTO ...}
 * which works on H2 (MODE=MySQL) and MySQL 8.0+; we deliberately avoid the
 * MySQL-specific {@code ON DUPLICATE KEY UPDATE} extension so the same statement
 * runs across both vendors. Concurrent writers for the same {@code adapterId}
 * are serialized at the row-level lock; since the scheduler holds the
 * distributed lock per adapter before reaching here, conflicts are not expected
 * but the MERGE is correct under contention regardless.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
@Profile("!test")
public class JdbcWatermarkStore implements WatermarkStore {

    private static final Logger LOG = LoggerFactory.getLogger(JdbcWatermarkStore.class);

    /** Vendor-portable UPSERT — H2 MODE=MySQL and MySQL 8.0+ both accept MERGE INTO. */
    private static final String SQL_UPSERT =
            "MERGE INTO collection_record_offset (adapter_id, watermark, updated_at) "
                    + "KEY(adapter_id) VALUES (?, ?, ?)";

    private static final String SQL_SELECT =
            "SELECT watermark FROM collection_record_offset WHERE adapter_id = ?";

    private final JdbcTemplate jdbcTemplate;

    /**
     * Spring constructor injection.
     *
     * @param jdbcTemplate non-null Spring {@link JdbcTemplate}
     */
    public JdbcWatermarkStore(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
    }

    @Override
    public Optional<String> get(final String adapterId) {
        Objects.requireNonNull(adapterId, "adapterId");
        try {
            final String wm = jdbcTemplate.queryForObject(SQL_SELECT, String.class, adapterId);
            // queryForObject returns null when the column itself is SQL NULL — schema
            // declares NOT NULL so this is a defensive guard, not a possible state.
            return Optional.ofNullable(wm);
        } catch (EmptyResultDataAccessException notFound) {
            // First run for this adapterId — caller falls back to initialWatermark.
            return Optional.empty();
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
        try {
            jdbcTemplate.update(SQL_UPSERT, adapterId, watermark, Timestamp.from(Instant.now()));
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
