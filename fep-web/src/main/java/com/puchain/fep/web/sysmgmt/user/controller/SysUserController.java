package com.puchain.fep.web.sysmgmt.user.controller;

import com.puchain.fep.common.domain.ApiResult;
import com.puchain.fep.common.domain.PageResult;
import com.puchain.fep.web.sysmgmt.user.domain.UserStatus;
import com.puchain.fep.web.sysmgmt.user.dto.ResetPasswordRequest;
import com.puchain.fep.web.sysmgmt.user.dto.UserCreateRequest;
import com.puchain.fep.web.sysmgmt.user.dto.UserResponse;
import com.puchain.fep.web.sysmgmt.user.dto.UserUpdateRequest;
import com.puchain.fep.web.sysmgmt.user.service.SysUserService;
import com.puchain.fep.web.sysmgmt.log.annotation.OperationLog;
import com.puchain.fep.web.sysmgmt.log.domain.OperationType;
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

/**
 * 用户管理 REST API。
 *
 * <p>提供用户 CRUD、启用/禁用、重置密码接口。
 * 参见 PRD v1.3 §5.10.1 用户管理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/sys/users")
@Tag(name = "02. 用户管理", description = "用户 CRUD / 启用禁用 / 重置密码")
public class SysUserController {

    private final SysUserService userService;

    /**
     * 构造 SysUserController。
     *
     * @param userService 用户管理服务
     */
    public SysUserController(final SysUserService userService) {
        this.userService = userService;
    }

    /**
     * 搜索用户（分页）。
     *
     * @param keyword  关键字（可选，匹配用户名或账号）
     * @param pageNum  页码（1-based，默认 1）
     * @param pageSize 每页大小（默认 20）
     * @return 分页用户列表
     */
    @GetMapping
    @OperationLog(module = "用户管理", type = OperationType.QUERY, description = "搜索用户")
    @Operation(summary = "搜索用户", description = "按用户名或账号关键字分页搜索")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public ApiResult<PageResult<UserResponse>> search(
            @Parameter(description = "关键字") @RequestParam(required = false) final String keyword,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") final int pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") final int pageSize) {
        return ApiResult.success(userService.search(keyword, pageNum, pageSize));
    }

    /**
     * 查看用户详情。
     *
     * @param userId 用户 ID
     * @return 用户详情
     */
    @GetMapping("/{userId}")
    @Operation(summary = "查看用户详情", description = "根据用户 ID 查询用户信息")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @ApiResponse(responseCode = "404", description = "用户不存在")
    public ApiResult<UserResponse> findById(
            @Parameter(description = "用户 ID") @PathVariable final String userId) {
        return ApiResult.success(userService.findById(userId));
    }

    /**
     * 创建用户。
     *
     * @param request 创建请求
     * @return 新建用户信息
     */
    @PostMapping
    @OperationLog(module = "用户管理", type = OperationType.CREATE, description = "创建用户")
    @Operation(summary = "创建用户", description = "新增用户，登录账号不可重复")
    @ApiResponse(responseCode = "200", description = "创建成功")
    @ApiResponse(responseCode = "400", description = "参数校验失败")
    @ApiResponse(responseCode = "409", description = "账号已存在")
    public ApiResult<UserResponse> create(@Valid @RequestBody final UserCreateRequest request) {
        return ApiResult.success(userService.create(request));
    }

    /**
     * 更新用户信息。
     *
     * @param userId  用户 ID
     * @param request 更新请求
     * @return 更新后的用户信息
     */
    @PutMapping("/{userId}")
    @OperationLog(module = "用户管理", type = OperationType.UPDATE, description = "更新用户")
    @Operation(summary = "更新用户", description = "修改用户姓名、手机号、邮箱、部门、角色")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @ApiResponse(responseCode = "404", description = "用户不存在")
    public ApiResult<UserResponse> update(
            @Parameter(description = "用户 ID") @PathVariable final String userId,
            @Valid @RequestBody final UserUpdateRequest request) {
        return ApiResult.success(userService.update(userId, request));
    }

    /**
     * 删除用户。
     *
     * @param userId 用户 ID
     * @return 空响应
     */
    @DeleteMapping("/{userId}")
    @OperationLog(module = "用户管理", type = OperationType.DELETE, description = "删除用户")
    @Operation(summary = "删除用户", description = "删除用户及关联角色绑定")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @ApiResponse(responseCode = "404", description = "用户不存在")
    public ApiResult<Void> delete(
            @Parameter(description = "用户 ID") @PathVariable final String userId) {
        userService.delete(userId);
        return ApiResult.success();
    }

    /**
     * 启用用户。
     *
     * @param userId 用户 ID
     * @return 更新后的用户信息
     */
    @PostMapping("/{userId}/enable")
    @OperationLog(module = "用户管理", type = OperationType.UPDATE, description = "启用用户")
    @Operation(summary = "启用用户", description = "将用户状态设为 ACTIVE，清除锁定")
    @ApiResponse(responseCode = "200", description = "启用成功")
    @ApiResponse(responseCode = "404", description = "用户不存在")
    public ApiResult<UserResponse> enable(
            @Parameter(description = "用户 ID") @PathVariable final String userId) {
        return ApiResult.success(userService.setStatus(userId, UserStatus.ACTIVE));
    }

    /**
     * 禁用用户。
     *
     * @param userId 用户 ID
     * @return 更新后的用户信息
     */
    @PostMapping("/{userId}/disable")
    @OperationLog(module = "用户管理", type = OperationType.UPDATE, description = "禁用用户")
    @Operation(summary = "禁用用户", description = "将用户状态设为 DISABLED")
    @ApiResponse(responseCode = "200", description = "禁用成功")
    @ApiResponse(responseCode = "404", description = "用户不存在")
    public ApiResult<UserResponse> disable(
            @Parameter(description = "用户 ID") @PathVariable final String userId) {
        return ApiResult.success(userService.setStatus(userId, UserStatus.DISABLED));
    }

    /**
     * 重置密码。
     *
     * @param userId  用户 ID
     * @param request 重置密码请求
     * @return 空响应
     */
    @PostMapping("/{userId}/reset-password")
    @OperationLog(module = "用户管理", type = OperationType.UPDATE, description = "重置密码")
    @Operation(summary = "重置密码", description = "重置用户密码，下次登录须修改")
    @ApiResponse(responseCode = "200", description = "重置成功")
    @ApiResponse(responseCode = "400", description = "密码复杂度不满足")
    @ApiResponse(responseCode = "404", description = "用户不存在")
    public ApiResult<Void> resetPassword(
            @Parameter(description = "用户 ID") @PathVariable final String userId,
            @Valid @RequestBody final ResetPasswordRequest request) {
        userService.resetPassword(userId, request);
        return ApiResult.success();
    }
}
