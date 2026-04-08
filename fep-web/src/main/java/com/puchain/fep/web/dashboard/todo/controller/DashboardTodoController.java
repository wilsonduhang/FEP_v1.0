package com.puchain.fep.web.dashboard.todo.controller;

import com.puchain.fep.common.domain.ApiResult;
import com.puchain.fep.common.domain.PageResult;
import com.puchain.fep.web.common.SecurityContextHelper;
import com.puchain.fep.web.dashboard.todo.domain.TodoStatus;
import com.puchain.fep.web.dashboard.todo.dto.TodoCreateRequest;
import com.puchain.fep.web.dashboard.todo.dto.TodoResponse;
import com.puchain.fep.web.dashboard.todo.service.DashboardTodoService;
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
 * 首页待办事项管理 REST Controller。
 *
 * <p>提供待办事项 CRUD 及状态流转接口。
 * 参见 PRD v1.3 §5.2.2 待办事项区域（FR-WEB-DASH-TODO）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/dashboard/todos")
@Tag(name = "首页待办事项", description = "PRD §5.2.2 首页待办事项管理")
public class DashboardTodoController {

    private final DashboardTodoService todoService;

    /**
     * 构造 DashboardTodoController。
     *
     * @param todoService 待办事项管理服务
     */
    public DashboardTodoController(final DashboardTodoService todoService) {
        this.todoService = todoService;
    }

    /**
     * 搜索待办事项（分页）。
     *
     * @param status   待办状态过滤（可选）
     * @param pageNum  页码（1-based，默认 1）
     * @param pageSize 每页大小（默认 10）
     * @return 分页待办列表
     */
    @GetMapping
    @OperationLog(module = "首页待办事项", type = OperationType.QUERY, description = "搜索待办事项")
    @Operation(summary = "搜索待办事项", description = "按状态过滤当前用户的待办列表，分页返回")
    @ApiResponse(responseCode = "200", description = "搜索成功")
    public ApiResult<PageResult<TodoResponse>> search(
            @Parameter(description = "待办状态过滤")
            @RequestParam(required = false) final TodoStatus status,
            @Parameter(description = "页码（从1开始）")
            @RequestParam(defaultValue = "1") final int pageNum,
            @Parameter(description = "每页条数")
            @RequestParam(defaultValue = "10") final int pageSize) {
        String userId = SecurityContextHelper.currentUserId();
        return ApiResult.success(todoService.search(userId, status, pageNum, pageSize));
    }

    /**
     * 统计当前用户的 PENDING 待办数量。
     *
     * @return PENDING 待办数量
     */
    @GetMapping("/count")
    @OperationLog(module = "首页待办事项", type = OperationType.QUERY, description = "统计待办数量")
    @Operation(summary = "待办数量统计", description = "统计当前用户 PENDING 状态的待办数量")
    @ApiResponse(responseCode = "200", description = "统计成功")
    public ApiResult<Long> countPending() {
        String userId = SecurityContextHelper.currentUserId();
        return ApiResult.success(todoService.countPending(userId));
    }

    /**
     * 创建待办事项。
     *
     * @param request 创建请求
     * @return 新建待办信息
     */
    @PostMapping
    @OperationLog(module = "首页待办事项", type = OperationType.CREATE, description = "创建待办事项")
    @Operation(summary = "创建待办事项", description = "创建新的待办事项，初始状态为 PENDING")
    @ApiResponse(responseCode = "200", description = "创建成功")
    @ApiResponse(responseCode = "400", description = "参数校验失败")
    public ApiResult<TodoResponse> create(
            @Valid @RequestBody final TodoCreateRequest request) {
        String userId = SecurityContextHelper.currentUserId();
        return ApiResult.success(todoService.create(request, userId));
    }

    /**
     * 开始处理待办事项（PENDING→IN_PROCESS）。
     *
     * @param todoId 待办 ID
     * @return 更新后的待办信息
     */
    @PutMapping("/{todoId}/process")
    @OperationLog(module = "首页待办事项", type = OperationType.UPDATE, description = "开始处理待办")
    @Operation(summary = "开始处理待办", description = "将 PENDING 状态的待办标记为 IN_PROCESS")
    @ApiResponse(responseCode = "200", description = "操作成功")
    @ApiResponse(responseCode = "404", description = "待办不存在")
    @ApiResponse(responseCode = "409", description = "状态不允许此操作")
    public ApiResult<TodoResponse> startProcessing(
            @Parameter(description = "待办 ID") @PathVariable final String todoId) {
        return ApiResult.success(todoService.startProcessing(todoId));
    }

    /**
     * 完成待办事项。
     *
     * @param todoId 待办 ID
     * @return 更新后的待办信息
     */
    @PutMapping("/{todoId}/complete")
    @OperationLog(module = "首页待办事项", type = OperationType.UPDATE, description = "完成待办")
    @Operation(summary = "完成待办", description = "将待办标记为 COMPLETED，记录完成时间")
    @ApiResponse(responseCode = "200", description = "操作成功")
    @ApiResponse(responseCode = "404", description = "待办不存在")
    @ApiResponse(responseCode = "409", description = "已完成不可重复操作")
    public ApiResult<TodoResponse> complete(
            @Parameter(description = "待办 ID") @PathVariable final String todoId) {
        return ApiResult.success(todoService.complete(todoId));
    }

    /**
     * 删除待办事项。
     *
     * @param todoId 待办 ID
     * @return 空响应
     */
    @DeleteMapping("/{todoId}")
    @OperationLog(module = "首页待办事项", type = OperationType.DELETE, description = "删除待办事项")
    @Operation(summary = "删除待办事项", description = "物理删除待办事项")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @ApiResponse(responseCode = "404", description = "待办不存在")
    public ApiResult<Void> delete(
            @Parameter(description = "待办 ID") @PathVariable final String todoId) {
        todoService.delete(todoId);
        return ApiResult.success();
    }
}
