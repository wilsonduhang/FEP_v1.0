package com.puchain.fep.web.migration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Flyway V1-V18 迁移脚本验证。
 *
 * <p>验证核心表创建、种子数据插入，以及 V17 合成索引、V18 对账表
 * （reconciliation_records + clearing_instruction_records）存在性。</p>
 */
@SpringBootTest
class FlywayMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void tSysUserTableShouldExist() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_sys_user", Integer.class);
        assertNotNull(count);
    }

    @Test
    void tSysRoleTableShouldExist() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_sys_role", Integer.class);
        assertNotNull(count);
    }

    @Test
    void tSysMenuTableShouldExist() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_sys_menu", Integer.class);
        assertNotNull(count);
    }

    @Test
    void tSysUserRoleTableShouldExist() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_sys_user_role", Integer.class);
        assertNotNull(count);
    }

    @Test
    void tSysRolePermissionTableShouldExist() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_sys_role_permission", Integer.class);
        assertNotNull(count);
    }

    @Test
    void adminUserShouldBeSeeded() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_sys_user WHERE user_account = 'admin1'", Integer.class);
        assertNotNull(count);
        assertEquals(1, count);
    }

    @Test
    void systemAdminRoleShouldBeSeeded() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_sys_role WHERE role_code = 'SYSTEM_ADMIN'", Integer.class);
        assertEquals(1, count);
    }

    @Test
    void topLevelMenusShouldBeSeeded() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_sys_menu WHERE menu_level = 1", Integer.class);
        assertEquals(9, count);
    }

    @ParameterizedTest(name = "V17 index {0} should exist")
    @ValueSource(strings = {
            "IDX_SUB_RECORD_PUSH_STATUS_CREATE_TIME",
            "IDX_SUB_RECORD_BIZ_TYPE_CREATE_TIME"
    })
    void v17IndexesShouldExist(final String indexName) {
        Integer distinctCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT INDEX_NAME) FROM INFORMATION_SCHEMA.INDEXES "
                        + "WHERE UPPER(INDEX_NAME) = ?",
                Integer.class, indexName);
        assertNotNull(distinctCount);
        assertEquals(1, distinctCount);
    }

    @ParameterizedTest(name = "V18 table {0} should exist")
    @ValueSource(strings = {"reconciliation_records", "clearing_instruction_records"})
    void v18TablesShouldExist(final String tableName) {
        assertTrue(tableExists(tableName), "expected table " + tableName + " to be created by V18");
    }

    @ParameterizedTest(name = "V18 index {0} should exist")
    @ValueSource(strings = {
            "IDX_RECON_DATE_STATUS",
            "IDX_RECON_MESSAGE_TYPE_DATE",
            "IDX_RECON_PAIRED_SERIAL",
            "IDX_CLEARING_STATUS_DATE",
            "IDX_CLEARING_MESSAGE_ID"
    })
    void v18IndexesShouldExist(final String indexName) {
        assertTrue(indexExists(indexName), "expected index " + indexName + " to be created by V18");
    }

    @ParameterizedTest(name = "V18 CHECK constraint {0} should exist")
    @ValueSource(strings = {
            "CHK_RECON_MESSAGE_TYPE",
            "CHK_RECON_STATUS",
            "CHK_CLEARING_TYPE",
            "CHK_CLEARING_STATUS"
    })
    void v18CheckConstraintsShouldExist(final String constraintName) {
        assertTrue(checkConstraintExists(constraintName),
                "expected CHECK constraint " + constraintName + " to be enforced by V18");
    }

    @Test
    void v18UniqueConstraintShouldExist() {
        assertTrue(uniqueConstraintExists("UQ_RECON_SERIAL_MESSAGE"),
                "expected UNIQUE constraint uq_recon_serial_message to be enforced by V18");
    }

    /**
     * Checks whether a table exists via H2 / MySQL compatible {@code INFORMATION_SCHEMA.TABLES}.
     *
     * @param tableName logical table name (case-insensitive match)
     * @return {@code true} if the table is present in the current schema
     */
    private boolean tableExists(final String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES "
                        + "WHERE UPPER(TABLE_NAME) = UPPER(?)",
                Integer.class, tableName);
        return count != null && count > 0;
    }

    /**
     * Checks whether an index exists via H2 / MySQL compatible {@code INFORMATION_SCHEMA.INDEXES}.
     *
     * @param indexName logical index name (case-insensitive match)
     * @return {@code true} if the index is present in any user table
     */
    private boolean indexExists(final String indexName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT INDEX_NAME) FROM INFORMATION_SCHEMA.INDEXES "
                        + "WHERE UPPER(INDEX_NAME) = UPPER(?)",
                Integer.class, indexName);
        return count != null && count > 0;
    }

    /**
     * Checks whether a named CHECK constraint exists. Compatible with H2 (MODE=MySQL) and MySQL 8.0+
     * via {@code INFORMATION_SCHEMA.CHECK_CONSTRAINTS}.
     *
     * @param constraintName logical CHECK constraint name (case-insensitive match)
     * @return {@code true} if the constraint is enforced
     */
    private boolean checkConstraintExists(final String constraintName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.CHECK_CONSTRAINTS "
                        + "WHERE UPPER(CONSTRAINT_NAME) = UPPER(?)",
                Integer.class, constraintName);
        return count != null && count > 0;
    }

    /**
     * Checks whether a named UNIQUE constraint exists via {@code INFORMATION_SCHEMA.TABLE_CONSTRAINTS}.
     *
     * @param constraintName logical UNIQUE constraint name (case-insensitive match)
     * @return {@code true} if the constraint is enforced
     */
    private boolean uniqueConstraintExists(final String constraintName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS "
                        + "WHERE UPPER(CONSTRAINT_NAME) = UPPER(?) AND CONSTRAINT_TYPE = 'UNIQUE'",
                Integer.class, constraintName);
        return count != null && count > 0;
    }
}
