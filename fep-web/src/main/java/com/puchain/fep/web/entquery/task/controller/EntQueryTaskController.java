package com.puchain.fep.web.entquery.task.controller;

import com.puchain.fep.common.domain.ApiResult;
import com.puchain.fep.common.domain.PageResult;
import com.puchain.fep.web.entquery.task.dto.QueryTaskCreateRequest;
import com.puchain.fep.web.entquery.task.dto.QueryTaskResponse;
import com.puchain.fep.web.entquery.task.service.EntQueryTaskService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 企业查询任务管理 REST API。
 *
 * <p>提供查询任务 CRUD 及执行接口。
 * 参见 PRD v1.3 §5.4 企业信息查询管理（FR-WEB-ENT）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/ent-query/tasks")
@Tag(name = "20. 企业查询 — 查询任务", description = "查询任务 CRUD + 执行")
public class EntQueryTaskController {

    private final EntQueryTaskService taskService;

    /**
     * 构造 EntQueryTaskController。
     *
     * @param taskService 查询任务管理服务
     */
    public EntQueryTaskController(final EntQueryTaskService taskService) {
        this.taskService = taskService;
    }

    /**
     * 搜索查询任务（分页）。
     *
     * @param queryType  查询类型（可选，REALTIME/BATCH）
     * @param taskStatus 任务状态（可选，DRAFT/PROCESSING/COMPLETED/FAILED）
     * @param keyword    关键字（可选，匹配 USCI 或企业名称）
     * @param pageNum    页码（1-based，默认 1）
     * @param pageSize   每页大小（默认 10）
     * @return 分页查询任务列表
     */
    @GetMapping
    @OperationLog(module = "查询任务管理", type = OperationType.QUERY, description = "搜索查询任务")
    @Operation(summary = "搜索查询任务", description = "按查询类型/状态/关键字分页搜索")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public ApiResult<PageResult<QueryTaskResponse>> search(
            @Parameter(description = "查询类型") @RequestParam(required = false) final String queryType,
            @Parameter(description = "任务状态") @RequestParam(required = false) final String taskStatus,
            @Parameter(description = "关键字") @RequestParam(required = false) final String keyword,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") final int pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") final int pageSize) {
        return ApiResult.success(taskService.search(queryType, taskStatus, keyword, pageNum, pageSize));
    }

    /**
     * 按 ID 查询任务详情。
     *
     * @param taskId 任务 ID
     * @return 查询任务详情
     */
    @GetMapping("/{taskId}")
    @OperationLog(module = "查询任务管理", type = OperationType.QUERY, description = "查询任务详情")
    @Operation(summary = "查询任务详情", description = "按 ID 查询查询任务")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @ApiResponse(responseCode = "404", description = "任务不存在")
    public ApiResult<QueryTaskResponse> getById(
            @Parameter(description = "任务 ID") @PathVariable final String taskId) {
        return ApiResult.success(taskService.getById(taskId));
    }

    /**
     * 创建查询任务。
     *
     * @param request 创建请求
     * @return 新建查询任务信息
     */
    @PostMapping
    @OperationLog(module = "查询任务管理", type = OperationType.CREATE, description = "创建查询任务")
    @Operation(summary = "创建查询任务", description = "新建查询任务，初始状态为 DRAFT")
    @ApiResponse(responseCode = "200", description = "创建成功")
    @ApiResponse(responseCode = "400", description = "参数校验失败")
    @ApiResponse(responseCode = "404", description = "企业主体不存在")
    public ApiResult<QueryTaskResponse> create(
            @Valid @RequestBody final QueryTaskCreateRequest request) {
        return ApiResult.success(taskService.create(request));
    }

    /**
     * 执行查询任务（DRAFT -> PROCESSING）。
     *
     * @param taskId 任务 ID
     * @return 更新后的查询任务信息
     */
    @PostMapping("/{taskId}/execute")
    @OperationLog(module = "查询任务管理", type = OperationType.UPDATE, description = "执行查询任务")
    @Operation(summary = "执行查询任务", description = "将 DRAFT 状态任务变更为 PROCESSING")
    @ApiResponse(responseCode = "200", description = "执行成功")
    @ApiResponse(responseCode = "400", description = "任务状态不允许执行")
    @ApiResponse(responseCode = "404", description = "任务不存在")
    public ApiResult<QueryTaskResponse> execute(
            @Parameter(description = "任务 ID") @PathVariable final String taskId) {
        return ApiResult.success(taskService.execute(taskId));
    }

    /**
     * 删除查询任务（仅 DRAFT 状态）。
     *
     * @param taskId 任务 ID
     * @return 空响应
     */
    @DeleteMapping("/{taskId}")
    @OperationLog(module = "查询任务管理", type = OperationType.DELETE, description = "删除查询任务")
    @Operation(summary = "删除查询任务", description = "仅 DRAFT 状态的任务可删除")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @ApiResponse(responseCode = "400", description = "任务状态不允许删除")
    @ApiResponse(responseCode = "404", description = "任务不存在")
    public ApiResult<Void> delete(
            @Parameter(description = "任务 ID") @PathVariable final String taskId) {
        taskService.delete(taskId);
        return ApiResult.success();
    }
}
