package com.puchain.fep.web.migration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 V7 迁移脚本正确插入 P6a.3 菜单和权限种子数据。
 *
 * <p>使用 {@code @SpringBootTest} 而非 {@code @DataJpaTest}，因为 H2 MODE=MySQL
 * 的 COMMENT 语法需要完整应用上下文（与 V6MigrationTest 保持一致）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
class V7MigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void v7Migration_shouldInsertSysConfigParentMenu() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_sys_menu WHERE menu_name = '系统配置'",
                Integer.class);
        assertThat(count).isGreaterThanOrEqualTo(1);
    }

    @Test
    void v7Migration_shouldInsertChildMenus() {
        Integer childCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_sys_menu WHERE menu_name IN ("
                + "'平台基础设置','业务类型管理','数据接收方管理','推送接口管理','接口预警管理',"
                + "'输出类型管理','数据类型管理','企业主体管理','其他系统配置')",
                Integer.class);
        assertThat(childCount).isEqualTo(9);
    }

    @Test
    void v7Migration_shouldInsertAdminRolePermissions() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_sys_role_permission rp "
                + "JOIN t_sys_menu m ON rp.menu_id = m.menu_id "
                + "WHERE m.menu_name IN ('系统配置','平台基础设置','业务类型管理','数据接收方管理',"
                + "'推送接口管理','接口预警管理','输出类型管理','数据类型管理','企业主体管理','其他系统配置')",
                Integer.class);
        assertThat(count).isGreaterThanOrEqualTo(10);
    }
}
