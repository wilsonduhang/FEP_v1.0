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
 *   <li>Flyway migrations create {@code t_sys_enterprise} on H2.</li>
 *   <li>Two APPROVED enterprise rows land in the table after startup.</li>
 * </ol>
 *
 * <p>The {@code test} profile supplies an isolated H2 URL; Redis is
 * mocked via {@link TestRedisConfiguration}.</p>
 *
 * @since P7.2b
 */
@SpringBootTest
@ActiveProfiles({"test", "dev", "dev-e2e"})
@Import(TestRedisConfiguration.class)
class E2eSeedRunnerIT {

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
}
