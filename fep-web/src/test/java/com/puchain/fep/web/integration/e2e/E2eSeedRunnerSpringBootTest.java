package com.puchain.fep.web.integration.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.puchain.fep.web.config.TestRedisConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test for {@link E2eSeedRunner}.
 *
 * <p>Boots the full Spring context under {@code test} + {@code dev-e2e}
 * profiles (with {@code dev} included so the security mock beans activate),
 * verifying that:</p>
 * <ol>
 *   <li>The {@code dev-e2e} profile activates the {@link E2eSeedRunner} bean.</li>
 *   <li>Flyway migrations create {@code t_sys_enterprise} and {@code t_tlq_node}
 *       on H2.</li>
 *   <li>Two APPROVED enterprise rows land in {@code t_sys_enterprise} after
 *       startup (matching {@code usci IN (91110000MA01A00001, 91110000MA01A00002)}).</li>
 *   <li>Two TLQ node rows land in {@code t_tlq_node} after startup (one
 *       {@code MASTER_PRODUCER} + one {@code SLAVE_CONSUMER}) so that P7.2d
 *       Playwright scenarios S6/S7/S9/S10 can exercise node-dependent behavior
 *       instead of gracefully skipping.</li>
 * </ol>
 *
 * <p>The {@code test} profile supplies an isolated H2 URL; Redis is
 * mocked via {@link TestRedisConfiguration}.</p>
 *
 * @since P7.2b (enterprise assertion); TLQ assertion added P7.2d+1 (ca6d66d)
 */
@SpringBootTest
@ActiveProfiles({"test", "dev", "dev-e2e"})
@Import(TestRedisConfiguration.class)
class E2eSeedRunnerSpringBootTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void contextStartsAndSeedsApprovedEnterprises() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_sys_enterprise "
                        + "WHERE usci IN ('91110000MA01A00001','91110000MA01A00002') "
                        + "AND audit_status = 'APPROVED'",
                Integer.class);
        assertThat(count).isEqualTo(2);
    }

    @Test
    void contextStartsAndSeedsTlqNodes() {
        // Two rows expected per E2eSeedRunner.TLQ_NODE_SEED_ROWS:
        // (E2E TLQ Master, MASTER_PRODUCER) + (E2E TLQ Slave, SLAVE_CONSUMER).
        Integer total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_tlq_node "
                        + "WHERE node_name IN ('E2E TLQ Master','E2E TLQ Slave')",
                Integer.class);
        assertThat(total).isEqualTo(2);

        Integer master = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_tlq_node "
                        + "WHERE node_name = 'E2E TLQ Master' "
                        + "AND node_role = 'MASTER_PRODUCER'",
                Integer.class);
        assertThat(master).isEqualTo(1);

        Integer slave = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_tlq_node "
                        + "WHERE node_name = 'E2E TLQ Slave' "
                        + "AND node_role = 'SLAVE_CONSUMER'",
                Integer.class);
        assertThat(slave).isEqualTo(1);
    }
}
