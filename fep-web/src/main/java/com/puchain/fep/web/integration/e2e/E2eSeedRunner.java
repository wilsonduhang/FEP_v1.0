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
 * dev-e2e profile seed runner — upserts minimal fixture rows required by
 * Playwright scenarios. Currently seeds {@code t_sys_enterprise} (P7.2a
 * scenarios 4-7) and {@code t_tlq_node} (P7.2d scenarios S6/S7/S9/S10).
 * Uses plain JdbcTemplate SELECT+INSERT for H2/MySQL/PG dialect neutrality;
 * no Flyway (F-level freeze).
 *
 * <p>Idempotent: each seed row's UNIQUE key is counted before insert; rows
 * already present are skipped. Emits separate summary INFO log lines per
 * seed table in the canonical {@code [E2E-SEED-<TABLE>] applied: N rows
 * (inserted=X, skipped=Y, total=Z)} format for automated smoke verification.</p>
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

    // ---- Enterprise seed ----
    private static final String COUNT_ENTERPRISE_SQL =
            "SELECT COUNT(*) FROM t_sys_enterprise WHERE usci = ?";

    private static final String INSERT_ENTERPRISE_SQL = "INSERT INTO t_sys_enterprise("
            + "enterprise_id, enterprise_name, usci, audit_status, "
            + "biz_count, create_time, update_time) "
            + "VALUES (?, ?, ?, 'APPROVED', 0, ?, ?)";

    private static final String[][] ENTERPRISE_SEED_ROWS = {
        {"91110000MA01A00001", "E2E 测试企业 A"},
        {"91110000MA01A00002", "E2E 测试企业 B"},
    };

    // ---- TLQ node seed (P7.2d S6/S7/S9/S10 unblock) ----
    private static final String COUNT_TLQ_NODE_SQL =
            "SELECT COUNT(*) FROM t_tlq_node WHERE node_name = ?";

    private static final String INSERT_TLQ_NODE_SQL = "INSERT INTO t_tlq_node("
            + "node_id, node_name, node_role, host_ip, port, "
            + "create_time, update_time) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final TlqNodeSeedRow[] TLQ_NODE_SEED_ROWS = {
        new TlqNodeSeedRow("E2E TLQ Master", "MASTER_PRODUCER", "10.0.0.101", 20001),
        new TlqNodeSeedRow("E2E TLQ Slave", "SLAVE_CONSUMER", "10.0.0.102", 20002),
    };

    /** Immutable fixture row for {@code t_tlq_node}. */
    private record TlqNodeSeedRow(String nodeName, String nodeRole, String hostIp, int port) { }

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
     * Upsert the seed rows for all E2E-dependent tables. Any JDBC exception
     * propagates so Spring boot startup fails fast under a misconfigured
     * datasource.
     *
     * @param args ignored CLI arguments
     */
    @Override
    public void run(final String... args) {
        seedEnterprises();
        seedTlqNodes();
    }

    private void seedEnterprises() {
        int inserted = 0;
        int skipped = 0;
        for (String[] row : ENTERPRISE_SEED_ROWS) {
            Integer existing = jdbcTemplate.queryForObject(
                    COUNT_ENTERPRISE_SQL, Integer.class, row[0]);
            if (existing != null && existing > 0) {
                skipped++;
                continue;
            }
            try {
                Timestamp now = Timestamp.valueOf(LocalDateTime.now());
                jdbcTemplate.update(INSERT_ENTERPRISE_SQL,
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
        LOG.info("[E2E-SEED-ENTERPRISE] applied: {} rows (inserted={}, skipped={}, total={})",
                inserted + skipped, inserted, skipped, ENTERPRISE_SEED_ROWS.length);
    }

    private void seedTlqNodes() {
        int inserted = 0;
        int skipped = 0;
        for (TlqNodeSeedRow row : TLQ_NODE_SEED_ROWS) {
            Integer existing = jdbcTemplate.queryForObject(
                    COUNT_TLQ_NODE_SQL, Integer.class, row.nodeName());
            if (existing != null && existing > 0) {
                skipped++;
                continue;
            }
            try {
                Timestamp now = Timestamp.valueOf(LocalDateTime.now());
                jdbcTemplate.update(INSERT_TLQ_NODE_SQL,
                        UUID.randomUUID().toString().replace("-", ""),
                        row.nodeName(), row.nodeRole(), row.hostIp(), row.port(), now, now);
                inserted++;
            } catch (DuplicateKeyException dup) {
                // TOCTOU: concurrent startup inserted the row between SELECT and INSERT.
                // t_tlq_node.node_name and (host_ip, port) are UNIQUE, so count as skipped.
                skipped++;
            }
        }
        LOG.info("[E2E-SEED-TLQ] applied: {} rows (inserted={}, skipped={}, total={})",
                inserted + skipped, inserted, skipped, TLQ_NODE_SEED_ROWS.length);
    }
}
