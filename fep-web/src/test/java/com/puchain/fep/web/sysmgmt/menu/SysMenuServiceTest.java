package com.puchain.fep.web.sysmgmt.menu;

import com.puchain.fep.web.sysmgmt.menu.dto.MenuTreeNode;
import com.puchain.fep.web.sysmgmt.menu.service.SysMenuService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SysMenuService 集成测试。
 *
 * <p>使用 H2 内存数据库 + Flyway 种子数据验证菜单管理服务。</p>
 *
 * <p>V3 种子数据包含 9 个顶级菜单 + 7 个系统管理子菜单，
 * 超管角色已授予所有菜单 view 权限。</p>
 */
@SpringBootTest
@Transactional
class SysMenuServiceTest {

    /** 种子数据中超管用户 ID。 */
    private static final String ADMIN_USER_ID = "00000000000000000000000000000001";

    /** 种子数据中顶级菜单数量（V3 插入 9 个顶级菜单）。 */
    private static final int TOP_LEVEL_MENU_COUNT = 9;

    @Autowired
    private SysMenuService menuService;

    @Test
    void fullTreeShouldReturn9TopLevelMenus() {
        List<MenuTreeNode> tree = menuService.getFullTree();

        assertEquals(TOP_LEVEL_MENU_COUNT, tree.size(),
                "V3 种子数据应有 9 个顶级菜单");

        // 系统管理应有 7 个子菜单
        MenuTreeNode sysMgmt = tree.stream()
                .filter(n -> "SYS_MGMT".equals(n.getMenuCode()))
                .findFirst()
                .orElse(null);
        assertNotNull(sysMgmt, "应存在 SYS_MGMT 菜单");
        assertEquals(7, sysMgmt.getChildren().size(),
                "系统管理下应有 7 个子菜单");
    }

    @Test
    void adminUserTreeShouldReturn9TopLevelMenus() {
        List<MenuTreeNode> tree = menuService.getUserMenuTree(ADMIN_USER_ID);

        assertEquals(TOP_LEVEL_MENU_COUNT, tree.size(),
                "超管用户应能看到全部 9 个顶级菜单");
    }

    @Test
    void unknownUserTreeShouldBeEmpty() {
        List<MenuTreeNode> tree = menuService.getUserMenuTree("nonexistent_user_id");

        assertTrue(tree.isEmpty(), "不存在的用户应返回空菜单树");
    }
}
