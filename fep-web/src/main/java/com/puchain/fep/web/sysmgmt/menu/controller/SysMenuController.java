package com.puchain.fep.web.sysmgmt.menu.controller;

import com.puchain.fep.common.domain.ApiResult;
import com.puchain.fep.web.sysmgmt.menu.domain.SysMenu;
import com.puchain.fep.web.sysmgmt.menu.dto.MenuCreateRequest;
import com.puchain.fep.web.sysmgmt.menu.dto.MenuTreeNode;
import com.puchain.fep.web.sysmgmt.menu.service.SysMenuService;
import com.puchain.fep.web.sysmgmt.log.annotation.OperationLog;
import com.puchain.fep.web.sysmgmt.log.domain.OperationType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 菜单管理 REST API。
 *
 * <p>提供菜单树查询、CRUD、状态切换、排序调整接口。
 * 参见 PRD v1.3 §5.10.3 菜单管理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/sys/menus")
@Tag(name = "04. 菜单管理", description = "菜单树查询 / CRUD / 状态切换 / 排序调整")
public class SysMenuController {

    private final SysMenuService menuService;

    /**
     * 构造 SysMenuController。
     *
     * @param menuService 菜单管理服务
     */
    public SysMenuController(final SysMenuService menuService) {
        this.menuService = menuService;
    }

    /**
     * 获取完整菜单树（管理用）。
     *
     * @return 菜单树节点列表
     */
    @GetMapping("/tree")
    @OperationLog(module = "菜单管理", type = OperationType.QUERY, description = "查询完整菜单树")
    @Operation(summary = "获取完整菜单树", description = "管理用，返回所有状态的菜单树")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public ApiResult<List<MenuTreeNode>> getFullTree() {
        return ApiResult.success(menuService.getFullTree());
    }

    /**
     * 获取当前用户可访问的菜单树。
     *
     * @param userId 当前登录用户 ID（从 SecurityContext 注入）
     * @return 用户可访问的菜单树节点列表
     */
    @GetMapping("/my-tree")
    @Operation(summary = "获取当前用户菜单树", description = "根据用户角色权限过滤的菜单树")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public ApiResult<List<MenuTreeNode>> getMyTree(
            @AuthenticationPrincipal final String userId) {
        return ApiResult.success(menuService.getUserMenuTree(userId));
    }

    /**
     * 创建菜单。
     *
     * @param request 创建请求
     * @return 新建菜单树节点
     */
    @PostMapping
    @OperationLog(module = "菜单管理", type = OperationType.CREATE, description = "创建菜单")
    @Operation(summary = "创建菜单", description = "新增菜单节点，编码不可重复")
    @ApiResponse(responseCode = "200", description = "创建成功")
    @ApiResponse(responseCode = "400", description = "参数校验失败")
    @ApiResponse(responseCode = "409", description = "菜单编码已存在")
    public ApiResult<MenuTreeNode> create(@Valid @RequestBody final MenuCreateRequest request) {
        SysMenu menu = menuService.create(request);
        return ApiResult.success(MenuTreeNode.from(menu));
    }

    /**
     * 删除菜单（仅叶子节点）。
     *
     * @param menuId 菜单 ID
     * @return 空响应
     */
    @DeleteMapping("/{menuId}")
    @OperationLog(module = "菜单管理", type = OperationType.DELETE, description = "删除菜单")
    @Operation(summary = "删除菜单", description = "仅允许删除叶子节点")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @ApiResponse(responseCode = "400", description = "存在子菜单不允许删除")
    @ApiResponse(responseCode = "404", description = "菜单不存在")
    public ApiResult<Void> delete(
            @Parameter(description = "菜单 ID") @PathVariable final String menuId) {
        menuService.delete(menuId);
        return ApiResult.success();
    }

    /**
     * 切换菜单状态。
     *
     * @param menuId 菜单 ID
     * @return 更新后的菜单树节点
     */
    @PostMapping("/{menuId}/toggle-status")
    @OperationLog(module = "菜单管理", type = OperationType.UPDATE, description = "切换菜单状态")
    @Operation(summary = "切换菜单状态", description = "ACTIVE 与 DISABLED 互切")
    @ApiResponse(responseCode = "200", description = "切换成功")
    @ApiResponse(responseCode = "404", description = "菜单不存在")
    public ApiResult<MenuTreeNode> toggleStatus(
            @Parameter(description = "菜单 ID") @PathVariable final String menuId) {
        SysMenu menu = menuService.toggleStatus(menuId);
        return ApiResult.success(MenuTreeNode.from(menu));
    }

    /**
     * 更新菜单排序序号。
     *
     * @param menuId    菜单 ID
     * @param sortOrder 新排序序号
     * @return 更新后的菜单树节点
     */
    @PatchMapping("/{menuId}/sort-order")
    @OperationLog(module = "菜单管理", type = OperationType.UPDATE, description = "更新菜单排序")
    @Operation(summary = "更新菜单排序", description = "修改菜单的排序序号")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @ApiResponse(responseCode = "404", description = "菜单不存在")
    public ApiResult<MenuTreeNode> updateSortOrder(
            @Parameter(description = "菜单 ID") @PathVariable final String menuId,
            @Parameter(description = "新排序序号") @RequestParam final int sortOrder) {
        SysMenu menu = menuService.updateSortOrder(menuId, sortOrder);
        return ApiResult.success(MenuTreeNode.from(menu));
    }
}
