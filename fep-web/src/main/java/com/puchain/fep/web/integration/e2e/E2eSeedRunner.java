package com.puchain.fep.web.integration.e2e;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * dev-e2e profile seed runner — upserts minimal {@code t_sys_enterprise} rows
 * required by Playwright scenarios that reference an existing APPROVED
 * enterprise (P7.2a scenarios 4-7). Uses plain JdbcTemplate SELECT+INSERT
 * for H2/MySQL/PG dialect neutrality; no Flyway (F-level freeze).
 *
 * <p>Idempotent: each seed USCI is counted before insert; rows already
 * present are skipped. Logs a single summary line at INFO level in the
 * canonical {@code [E2E-SEED] applied: N rows (inserted=X, skipped=Y, total=Z)}
 * format for automated smoke verification.</p>
 *
 * <p>⚠️ ONLY activates under {@code dev-e2e} profile. Default {@code dev} /
 * {@code prod} profiles do not load this bean, so production and regular
 * development datasets remain untouched.</p>
 *
 * @since P7.2b
 */
@Component
@Profile("dev-e2e")
public class E2eSeedRunner implements CommandLineRunner {

    private static final Logger LOG = LoggerFactory.getLogger(E2eSeedRunner.class);

    private static final String COUNT_SQL = "SELECT COUNT(*) FROM t_sys_enterprise WHERE usci = ?";

    private static final String INSERT_SQL = "INSERT INTO t_sys_enterprise("
            + "enterprise_id, enterprise_name, usci, audit_status, "
            + "biz_count, create_time, update_time) "
            + "VALUES (?, ?, ?, 'APPROVED', 0, ?, ?)";

    private static final String[][] SEED_ROWS = {
        {"91110000MA01A00001", "E2E 测试企业 A"},
        {"91110000MA01A00002", "E2E 测试企业 B"},
    };

    private final JdbcTemplate jdbcTemplate;

    /**
     * Constructor injection of the Spring-managed JdbcTemplate.
     *
     * @param jdbcTemplate the auto-configured JdbcTemplate bean
     */
    public E2eSeedRunner(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Upsert the seed enterprise rows. Any JDBC exception propagates so
     * Spring boot startup fails fast under a misconfigured datasource.
     *
     * @param args ignored CLI arguments
     */
    @Override
    public void run(final String... args) {
        int inserted = 0;
        int skipped = 0;
        for (String[] row : SEED_ROWS) {
            Integer existing = jdbcTemplate.queryForObject(COUNT_SQL, Integer.class, row[0]);
            if (existing != null && existing > 0) {
                skipped++;
                continue;
            }
            try {
                Timestamp now = Timestamp.valueOf(LocalDateTime.now());
                jdbcTemplate.update(INSERT_SQL,
                        UUID.randomUUID().toString().replace("-", ""),
                        row[1], row[0], now, now);
                inserted++;
            } catch (DuplicateKeyException dup) {
                // TOCTOU: concurrent startup (or parallel bean init) inserted the row
                // between our SELECT COUNT and INSERT. t_sys_enterprise.usci is
                // UNIQUE, so we count the race as skipped rather than failing startup.
                skipped++;
            }
        }
        LOG.info("[E2E-SEED] applied: {} rows (inserted={}, skipped={}, total={})",
                inserted + skipped, inserted, skipped, SEED_ROWS.length);
    }
}
