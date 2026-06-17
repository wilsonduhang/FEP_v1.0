package com.puchain.fep.web.audit.review;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 验证 V42 审核菜单 seed：MSG_REVIEW 子菜单挂在 DATA_AUDIT 下，且超管获 view 权限。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
class MsgReviewMenuSeedTest {

    private static final String MENU_ID = "20000000000000000000000000000029";
    private static final String DATA_AUDIT_ID = "10000000000000000000000000000008";
    private static final String SUPER_ADMIN_ROLE_ID = "00000000000000000000000000000010";

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void msgReviewMenu_isSeededUnderDataAudit() {
        final Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM t_sys_menu WHERE menu_id = ? AND menu_code = 'MSG_REVIEW' "
                        + "AND parent_id = ? AND menu_status = 'ACTIVE'",
                Integer.class, MENU_ID, DATA_AUDIT_ID);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void superAdmin_hasViewPermissionOnMsgReviewMenu() {
        final Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM t_sys_role_permission WHERE role_id = ? AND menu_id = ? "
                        + "AND permission_code = 'view'",
                Integer.class, SUPER_ADMIN_ROLE_ID, MENU_ID);
        assertThat(count).isEqualTo(1);
    }
}
