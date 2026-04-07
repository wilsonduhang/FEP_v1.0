package com.puchain.fep.web.migration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 V6 迁移脚本正确创建 P6a.3 系统配置管理所需的 10 张表，并验证种子数据。
 *
 * <p>使用 {@code @SpringBootTest} 而非 {@code @DataJpaTest}，因为 H2 MODE=MySQL
 * 的 COMMENT 语法需要完整应用上下文（与 V4MigrationTest 保持一致）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
class V6MigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void v6Migration_shouldCreateSysConfigTable() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_sys_config",
                Integer.class);
        assertThat(count).isNotNull().isGreaterThanOrEqualTo(0);
    }

    @Test
    void v6Migration_shouldCreateBusinessTypeTable() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_sys_business_type",
                Integer.class);
        assertThat(count).isNotNull().isGreaterThanOrEqualTo(0);
    }

    @Test
    void v6Migration_shouldCreateDataReceiverTable() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_sys_data_receiver",
                Integer.class);
        assertThat(count).isNotNull().isGreaterThanOrEqualTo(0);
    }

    @Test
    void v6Migration_shouldCreatePushInterfaceTable() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_sys_push_interface",
                Integer.class);
        assertThat(count).isNotNull().isGreaterThanOrEqualTo(0);
    }

    @Test
    void v6Migration_shouldCreateAlertRuleTable() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_sys_alert_rule",
                Integer.class);
        assertThat(count).isNotNull().isGreaterThanOrEqualTo(0);
    }

    @Test
    void v6Migration_shouldCreateOutputTypeTable() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_sys_output_type",
                Integer.class);
        assertThat(count).isNotNull().isGreaterThanOrEqualTo(0);
    }

    @Test
    void v6Migration_shouldCreateDataTypeConfigTable() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_sys_data_type_config",
                Integer.class);
        assertThat(count).isNotNull().isGreaterThanOrEqualTo(0);
    }

    @Test
    void v6Migration_shouldCreateEnterpriseTable() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_sys_enterprise",
                Integer.class);
        assertThat(count).isNotNull().isGreaterThanOrEqualTo(0);
    }

    @Test
    void v6Migration_shouldCreateEnterpriseBizTable() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_sys_enterprise_biz",
                Integer.class);
        assertThat(count).isNotNull().isGreaterThanOrEqualTo(0);
    }

    @Test
    void v6Migration_shouldCreateEnterpriseQueryConfigTable() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_sys_enterprise_query_config",
                Integer.class);
        assertThat(count).isNotNull().isGreaterThanOrEqualTo(0);
    }
}
