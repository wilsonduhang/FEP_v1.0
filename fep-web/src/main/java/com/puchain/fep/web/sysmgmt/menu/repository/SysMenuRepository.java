package com.puchain.fep.web.sysmgmt.menu.repository;

import com.puchain.fep.web.sysmgmt.menu.domain.MenuStatus;
import com.puchain.fep.web.sysmgmt.menu.domain.SysMenu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * 菜单 Repository。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Repository
public interface SysMenuRepository extends JpaRepository<SysMenu, String> {

    /**
     * 按状态查找菜单（按 sortOrder 升序）。
     *
     * @param menuStatus 菜单状态
     * @return 菜单列表
     */
    List<SysMenu> findByMenuStatusOrderBySortOrderAsc(MenuStatus menuStatus);

    /**
     * 查找所有菜单（按 sortOrder 升序）。
     *
     * @return 菜单列表
     */
    List<SysMenu> findAllByOrderBySortOrderAsc();

    /**
     * 按父级 ID 查找子菜单。
     *
     * @param parentId 父级菜单 ID
     * @return 子菜单列表
     */
    List<SysMenu> findByParentId(String parentId);

    /**
     * 判断菜单编码是否已存在。
     *
     * @param menuCode 菜单编码
     * @return true 已存在
     */
    boolean existsByMenuCode(String menuCode);

    /**
     * 根据菜单 ID 集合批量查找。
     *
     * @param menuIds 菜单 ID 集合
     * @return 菜单列表
     */
    List<SysMenu> findByMenuIdIn(Collection<String> menuIds);
}
