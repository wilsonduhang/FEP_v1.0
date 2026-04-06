package com.puchain.fep.web.sysmgmt.role.controller;

import com.puchain.fep.common.domain.ApiResult;
import com.puchain.fep.common.domain.PageResult;
import com.puchain.fep.web.sysmgmt.rel.domain.SysRolePermission;
import com.puchain.fep.web.sysmgmt.role.dto.RoleCreateRequest;
import com.puchain.fep.web.sysmgmt.role.dto.RolePermissionAssignRequest;
import com.puchain.fep.web.sysmgmt.role.dto.RoleResponse;
import com.puchain.fep.web.sysmgmt.role.dto.RoleUpdateRequest;
import com.puchain.fep.web.sysmgmt.role.service.SysRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 角色管理 REST API。
 *
 * <p>提供角色 CRUD、状态切换、权限分配接口。
 * 参见 PRD v1.3 §5.10.2 角色管理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/sys/roles")
@Tag(name = "03. 角色管理", description = "角色 CRUD / 状态切换 / 权限分配")
public class SysRoleController {

    private final SysRoleService roleService;

    /**
     * 构造 SysRoleController。
     *
     * @param roleService 角色管理服务
     */
    public SysRoleController(final SysRoleService roleService) {
        this.roleService = roleService;
    }

    /**
     * 创建角色。
     *
     * @param request 创建请求
     * @return 新建角色信息
     */
    @PostMapping
    @Operation(summary = "创建角色", description = "新增角色，角色编码不可重复")
    @ApiResponse(responseCode = "200", description = "创建成功")
    @ApiResponse(responseCode = "400", description = "参数校验失败")
    @ApiResponse(responseCode = "409", description = "角色编码已存在")
    public ApiResult<RoleResponse> create(@Valid @RequestBody final RoleCreateRequest request) {
        return ApiResult.success(roleService.create(request));
    }

    /**
     * 更新角色。
     *
     * @param roleId  角色 ID
     * @param request 更新请求
     * @return 更新后的角色信息
     */
    @PutMapping("/{roleId}")
    @Operation(summary = "更新角色", description = "修改角色名称、数据权限范围、备注")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @ApiResponse(responseCode = "404", description = "角色不存在")
    public ApiResult<RoleResponse> update(
            @Parameter(description = "角色 ID") @PathVariable final String roleId,
            @Valid @RequestBody final RoleUpdateRequest request) {
        return ApiResult.success(roleService.update(roleId, request));
    }

    /**
     * 删除角色（系统角色不可删除）。
     *
     * @param roleId 角色 ID
     * @return 空响应
     */
    @DeleteMapping("/{roleId}")
    @Operation(summary = "删除角色", description = "删除角色及关联权限/用户绑定，系统内置角色不可删除")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @ApiResponse(responseCode = "400", description = "系统角色不允许删除")
    @ApiResponse(responseCode = "404", description = "角色不存在")
    public ApiResult<Void> delete(
            @Parameter(description = "角色 ID") @PathVariable final String roleId) {
        roleService.delete(roleId);
        return ApiResult.success();
    }

    /**
     * 切换角色状态。
     *
     * @param roleId 角色 ID
     * @return 切换后的角色信息
     */
    @PutMapping("/{roleId}/toggle-status")
    @Operation(summary = "切换角色状态", description = "ACTIVE 与 DISABLED 互切")
    @ApiResponse(responseCode = "200", description = "切换成功")
    @ApiResponse(responseCode = "404", description = "角色不存在")
    public ApiResult<RoleResponse> toggleStatus(
            @Parameter(description = "角色 ID") @PathVariable final String roleId) {
        return ApiResult.success(roleService.toggleStatus(roleId));
    }

    /**
     * 搜索角色（分页）。
     *
     * @param keyword  角色名称关键字（可选）
     * @param pageNum  页码（1-based，默认 1）
     * @param pageSize 每页大小（默认 20）
     * @return 分页角色列表
     */
    @GetMapping
    @Operation(summary = "搜索角色", description = "按角色名称关键字分页搜索")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public ApiResult<PageResult<RoleResponse>> search(
            @Parameter(description = "角色名称关键字") @RequestParam(required = false) final String keyword,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") final int pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") final int pageSize) {
        return ApiResult.success(roleService.search(keyword, pageNum, pageSize));
    }

    /**
     * 分配角色权限（全量替换）。
     *
     * @param roleId  角色 ID
     * @param request 权限分配请求
     * @return 空响应
     */
    @PutMapping("/{roleId}/permissions")
    @Operation(summary = "分配角色权限", description = "全量替换角色的菜单权限")
    @ApiResponse(responseCode = "200", description = "分配成功")
    @ApiResponse(responseCode = "404", description = "角色不存在")
    public ApiResult<Void> assignPermissions(
            @Parameter(description = "角色 ID") @PathVariable final String roleId,
            @Valid @RequestBody final RolePermissionAssignRequest request) {
        roleService.assignPermissions(roleId, request);
        return ApiResult.success();
    }

    /**
     * 获取角色已分配的权限。
     *
     * @param roleId 角色 ID
     * @return 权限列表
     */
    @GetMapping("/{roleId}/permissions")
    @Operation(summary = "获取角色权限", description = "查询角色已分配的菜单权限列表")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @ApiResponse(responseCode = "404", description = "角色不存在")
    public ApiResult<List<SysRolePermission>> getPermissions(
            @Parameter(description = "角色 ID") @PathVariable final String roleId) {
        return ApiResult.success(roleService.getPermissions(roleId));
    }
}
