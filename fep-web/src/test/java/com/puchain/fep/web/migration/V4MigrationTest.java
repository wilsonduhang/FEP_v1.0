package com.puchain.fep.web.migration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 V4 迁移脚本正确创建 P6a.2 所需的 5 张表。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
class V4MigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void v4Migration_shouldCreateMessageTable() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_sys_message",
                Integer.class);
        assertThat(count).isNotNull().isGreaterThanOrEqualTo(0);
    }

    @Test
    void v4Migration_shouldCreateMessageReadTable() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_sys_message_read",
                Integer.class);
        assertThat(count).isNotNull().isGreaterThanOrEqualTo(0);
    }

    @Test
    void v4Migration_shouldCreateDownloadTaskTable() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_sys_download_task",
                Integer.class);
        assertThat(count).isNotNull().isGreaterThanOrEqualTo(0);
    }

    @Test
    void v4Migration_shouldCreateOperationLogTable() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_sys_operation_log",
                Integer.class);
        assertThat(count).isNotNull().isGreaterThanOrEqualTo(0);
    }

    @Test
    void v4Migration_shouldCreateHelpContentTable() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_sys_help_content",
                Integer.class);
        assertThat(count).isNotNull().isGreaterThanOrEqualTo(0);
    }
}
