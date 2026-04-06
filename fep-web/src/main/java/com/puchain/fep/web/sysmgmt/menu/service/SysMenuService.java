package com.puchain.fep.web.sysmgmt.menu.service;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.web.sysmgmt.menu.domain.MenuStatus;
import com.puchain.fep.web.sysmgmt.menu.domain.SysMenu;
import com.puchain.fep.web.sysmgmt.menu.dto.MenuCreateRequest;
import com.puchain.fep.web.sysmgmt.menu.dto.MenuTreeNode;
import com.puchain.fep.web.sysmgmt.menu.repository.SysMenuRepository;
import com.puchain.fep.web.sysmgmt.rel.domain.SysRolePermission;
import com.puchain.fep.web.sysmgmt.rel.repository.SysRolePermissionRepository;
import com.puchain.fep.web.sysmgmt.rel.repository.SysUserRoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 菜单管理服务。
 *
 * <p>提供菜单树查询、CRUD、状态切换、排序调整功能。
 * 参见 PRD v1.3 §5.10.3 菜单管理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Service
public class SysMenuService {

    private static final Logger log = LoggerFactory.getLogger(SysMenuService.class);

    private final SysMenuRepository menuRepository;
    private final SysUserRoleRepository userRoleRepository;
    private final SysRolePermissionRepository rolePermissionRepository;

    /**
     * 构造 SysMenuService。
     *
     * @param menuRepository           菜单 Repository
     * @param userRoleRepository        用户-角色关联 Repository
     * @param rolePermissionRepository  角色-权限关联 Repository
     */
    public SysMenuService(final SysMenuRepository menuRepository,
                          final SysUserRoleRepository userRoleRepository,
                          final SysRolePermissionRepository rolePermissionRepository) {
        this.menuRepository = menuRepository;
        this.userRoleRepository = userRoleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
    }

    /**
     * 获取完整菜单树（管理用，包含所有状态）。
     *
     * @return 顶层菜单节点列表
     */
    public List<MenuTreeNode> getFullTree() {
        List<SysMenu> allMenus = menuRepository.findAllByOrderBySortOrderAsc();
        return buildTree(allMenus, null);
    }

    /**
     * 获取用户可访问的菜单树（仅 ACTIVE 状态）。
     *
     * <p>流程：用户 → 角色 → 权限（menuId） → 过滤 ACTIVE 菜单 → 构建树</p>
     *
     * @param userId 用户 ID
     * @return 用户可访问的顶层菜单节点列表
     */
    public List<MenuTreeNode> getUserMenuTree(final String userId) {
        // 1. 获取用户角色 ID 列表
        List<String> roleIds = userRoleRepository.findRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) {
            return List.of();
        }

        // 2. 获取角色可访问的菜单 ID 集合
        List<SysRolePermission> permissions = rolePermissionRepository.findByRoleIdIn(roleIds);
        Set<String> accessibleMenuIds = permissions.stream()
                .map(SysRolePermission::getMenuId)
                .collect(Collectors.toSet());
        if (accessibleMenuIds.isEmpty()) {
            return List.of();
        }

        // 3. 查询 ACTIVE 菜单并过滤
        List<SysMenu> activeMenus = menuRepository.findByMenuStatusOrderBySortOrderAsc(MenuStatus.ACTIVE);
        List<SysMenu> filteredMenus = activeMenus.stream()
                .filter(m -> accessibleMenuIds.contains(m.getMenuId()))
                .toList();

        return buildTree(filteredMenus, null);
    }

    /**
     * 创建菜单。
     *
     * @param request 创建请求
     * @return 新建菜单 Entity
     * @throws FepBusinessException 菜单编码已存在
     */
    @Transactional
    public SysMenu create(final MenuCreateRequest request) {
        if (menuRepository.existsByMenuCode(request.getMenuCode())) {
            throw new FepBusinessException(FepErrorCode.BIZ_5002,
                    "菜单编码已存在: " + request.getMenuCode());
        }

        SysMenu menu = new SysMenu();
        menu.setMenuId(UUID.randomUUID().toString().replace("-", ""));
        menu.setMenuCode(request.getMenuCode());
        menu.setMenuName(request.getMenuName());
        menu.setParentId(request.getParentId());
        menu.setMenuLevel(request.getMenuLevel());
        menu.setMenuIcon(request.getMenuIcon());
        menu.setSortOrder(request.getSortOrder());
        menu.setMenuStatus(MenuStatus.ACTIVE);
        menu.setComponentPath(request.getComponentPath());
        menu.setRoutePath(request.getRoutePath());
        menu.setCreateTime(LocalDateTime.now());
        menu.setUpdateTime(LocalDateTime.now());

        SysMenu saved = menuRepository.save(menu);
        log.info("Menu created: code={}, level={}", saved.getMenuCode(), saved.getMenuLevel());
        return saved;
    }

    /**
     * 删除菜单（仅允许删除叶子节点）。
     *
     * @param menuId 菜单 ID
     * @throws FepBusinessException 菜单不存在或非叶子节点
     */
    @Transactional
    public void delete(final String menuId) {
        SysMenu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5001,
                        "菜单不存在: " + menuId));

        List<SysMenu> children = menuRepository.findByParentId(menuId);
        if (!children.isEmpty()) {
            throw new FepBusinessException(FepErrorCode.BIZ_5003,
                    "存在子菜单，不允许删除: " + menu.getMenuCode());
        }

        menuRepository.delete(menu);
        log.info("Menu deleted: code={}", menu.getMenuCode());
    }

    /**
     * 切换菜单状态（ACTIVE 与 DISABLED 互切）。
     *
     * @param menuId 菜单 ID
     * @return 更新后的菜单 Entity
     * @throws FepBusinessException 菜单不存在
     */
    @Transactional
    public SysMenu toggleStatus(final String menuId) {
        SysMenu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5001,
                        "菜单不存在: " + menuId));

        if (menu.getMenuStatus() == MenuStatus.ACTIVE) {
            menu.setMenuStatus(MenuStatus.DISABLED);
        } else {
            menu.setMenuStatus(MenuStatus.ACTIVE);
        }
        menu.setUpdateTime(LocalDateTime.now());

        SysMenu saved = menuRepository.save(menu);
        log.info("Menu status toggled: code={}, newStatus={}", saved.getMenuCode(), saved.getMenuStatus());
        return saved;
    }

    /**
     * 更新菜单排序序号。
     *
     * @param menuId    菜单 ID
     * @param sortOrder 新排序序号
     * @return 更新后的菜单 Entity
     * @throws FepBusinessException 菜单不存在
     */
    @Transactional
    public SysMenu updateSortOrder(final String menuId, final int sortOrder) {
        SysMenu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5001,
                        "菜单不存在: " + menuId));

        menu.setSortOrder(sortOrder);
        menu.setUpdateTime(LocalDateTime.now());

        SysMenu saved = menuRepository.save(menu);
        log.info("Menu sort order updated: code={}, sortOrder={}", saved.getMenuCode(), sortOrder);
        return saved;
    }

    /**
     * 构建菜单树。
     *
     * <p>将平铺列表按 parentId 分组后递归构建树结构。</p>
     *
     * @param all      所有菜单列表
     * @param parentId 根节点的父级 ID（null 表示从顶级开始）
     * @return 树节点列表
     */
    private List<MenuTreeNode> buildTree(final List<SysMenu> all, final String parentId) {
        Map<String, List<SysMenu>> byParent = all.stream()
                .collect(Collectors.groupingBy(
                        m -> m.getParentId() == null ? "" : m.getParentId()));
        return buildTreeRecursive(byParent, parentId == null ? "" : parentId);
    }

    /**
     * 递归构建树节点。
     *
     * @param byParent 按父级 ID 分组的菜单映射
     * @param parentId 当前层级的父级 ID
     * @return 当前层级的树节点列表
     */
    private List<MenuTreeNode> buildTreeRecursive(final Map<String, List<SysMenu>> byParent,
                                                  final String parentId) {
        return byParent.getOrDefault(parentId, List.of()).stream()
                .map(m -> {
                    MenuTreeNode node = MenuTreeNode.from(m);
                    buildTreeRecursive(byParent, m.getMenuId()).forEach(node::addChild);
                    return node;
                })
                .toList();
    }
}
