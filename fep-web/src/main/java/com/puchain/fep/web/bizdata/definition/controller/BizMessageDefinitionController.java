package com.puchain.fep.web.bizdata.definition.controller;

import com.puchain.fep.common.domain.ApiResult;
import com.puchain.fep.common.domain.EnableDisableStatus;
import com.puchain.fep.common.domain.PageResult;
import com.puchain.fep.web.bizdata.definition.dto.DefinitionCreateRequest;
import com.puchain.fep.web.bizdata.definition.dto.DefinitionResponse;
import com.puchain.fep.web.bizdata.definition.dto.DefinitionUpdateRequest;
import com.puchain.fep.web.bizdata.definition.service.BizMessageDefinitionService;
import com.puchain.fep.web.bizdata.domain.MessageDirection;
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
 * REST controller for business message definition management.
 *
 * <p>See PRD v1.3 section 5.3.1 + section 5.3.2 (FR-WEB-BIZ-LIST, FR-WEB-BIZ-DICT).</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/bizdata/definitions")
@Tag(name = "报文类型定义管理",
        description = "PRD section 5.3.1 + section 5.3.2 报文类型定义 CRUD + 启停")
public class BizMessageDefinitionController {

    private final BizMessageDefinitionService definitionService;

    /**
     * Construct BizMessageDefinitionController.
     *
     * @param definitionService message definition service
     */
    public BizMessageDefinitionController(
            final BizMessageDefinitionService definitionService) {
        this.definitionService = definitionService;
    }

    /**
     * Search message definitions (paginated) with optional filters.
     *
     * @param keyword          keyword matching messageCode or messageName (optional)
     * @param messageCode      exact message code filter (optional)
     * @param direction        message direction filter (optional)
     * @param definitionStatus definition status filter (optional)
     * @param pageNum          page number (1-based, default 1)
     * @param pageSize         page size (default 10)
     * @return paginated definition list
     */
    @GetMapping
    @OperationLog(module = "报文类型定义管理", type = OperationType.QUERY,
            description = "搜索报文类型定义")
    @Operation(summary = "搜索报文类型定义",
            description = "按报文编码或名称模糊搜索，支持精确过滤，分页返回")
    @ApiResponse(responseCode = "200", description = "搜索成功")
    public ApiResult<PageResult<DefinitionResponse>> search(
            @Parameter(description = "报文编码或名称关键字")
            @RequestParam(required = false) final String keyword,
            @Parameter(description = "报文编码精确过滤")
            @RequestParam(required = false) final String messageCode,
            @Parameter(description = "报文方向过滤")
            @RequestParam(required = false) final MessageDirection direction,
            @Parameter(description = "定义状态过滤")
            @RequestParam(required = false) final EnableDisableStatus definitionStatus,
            @Parameter(description = "页码（从1开始）")
            @RequestParam(defaultValue = "1") final int pageNum,
            @Parameter(description = "每页条数")
            @RequestParam(defaultValue = "10") final int pageSize) {
        return ApiResult.success(
                definitionService.search(keyword, messageCode, direction,
                        definitionStatus, pageNum, pageSize));
    }

    /**
     * Get a message definition by ID.
     *
     * @param definitionId definition ID
     * @return definition detail
     */
    @GetMapping("/{definitionId}")
    @OperationLog(module = "报文类型定义管理", type = OperationType.QUERY,
            description = "查询报文类型定义详情")
    @Operation(summary = "报文类型定义详情",
            description = "按 ID 查询报文类型定义，含 fieldSummary 和 sampleXml")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @ApiResponse(responseCode = "404", description = "报文类型不存在")
    public ApiResult<DefinitionResponse> getById(
            @Parameter(description = "定义 ID")
            @PathVariable final String definitionId) {
        return ApiResult.success(definitionService.getById(definitionId));
    }

    /**
     * Create a new message definition.
     *
     * @param request creation request
     * @return created definition
     */
    @PostMapping
    @OperationLog(module = "报文类型定义管理", type = OperationType.CREATE,
            description = "新增报文类型定义")
    @Operation(summary = "新增报文类型定义",
            description = "创建新的报文类型定义，messageCode 不能重复")
    @ApiResponse(responseCode = "200", description = "创建成功")
    @ApiResponse(responseCode = "400", description = "参数校验失败")
    @ApiResponse(responseCode = "409", description = "报文编码已存在")
    public ApiResult<DefinitionResponse> create(
            @Valid @RequestBody final DefinitionCreateRequest request) {
        return ApiResult.success(definitionService.create(request));
    }

    /**
     * Update a message definition.
     *
     * @param definitionId definition ID
     * @param request      update request
     * @return updated definition
     */
    @PutMapping("/{definitionId}")
    @OperationLog(module = "报文类型定义管理", type = OperationType.UPDATE,
            description = "编辑报文类型定义")
    @Operation(summary = "编辑报文类型定义",
            description = "修改报文类型定义，messageCode 唯一性校验排除自身")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @ApiResponse(responseCode = "400", description = "参数校验失败")
    @ApiResponse(responseCode = "404", description = "报文类型不存在")
    @ApiResponse(responseCode = "409", description = "报文编码已存在")
    public ApiResult<DefinitionResponse> update(
            @Parameter(description = "定义 ID")
            @PathVariable final String definitionId,
            @Valid @RequestBody final DefinitionUpdateRequest request) {
        return ApiResult.success(
                definitionService.update(definitionId, request));
    }

    /**
     * Toggle definition status (ENABLED to DISABLED or vice versa).
     *
     * @param definitionId definition ID
     * @return updated definition
     */
    @PutMapping("/{definitionId}/toggle-status")
    @OperationLog(module = "报文类型定义管理", type = OperationType.UPDATE,
            description = "切换报文类型定义状态")
    @Operation(summary = "启用/停用报文类型定义",
            description = "ENABLED 和 DISABLED 切换")
    @ApiResponse(responseCode = "200", description = "切换成功")
    @ApiResponse(responseCode = "404", description = "报文类型不存在")
    public ApiResult<DefinitionResponse> toggleStatus(
            @Parameter(description = "定义 ID")
            @PathVariable final String definitionId) {
        return ApiResult.success(definitionService.toggleStatus(definitionId));
    }

    /**
     * Delete a message definition.
     *
     * @param definitionId definition ID
     * @return empty response
     */
    @DeleteMapping("/{definitionId}")
    @OperationLog(module = "报文类型定义管理", type = OperationType.DELETE,
            description = "删除报文类型定义")
    @Operation(summary = "删除报文类型定义", description = "物理删除报文类型定义")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @ApiResponse(responseCode = "404", description = "报文类型不存在")
    public ApiResult<Void> delete(
            @Parameter(description = "定义 ID")
            @PathVariable final String definitionId) {
        definitionService.delete(definitionId);
        return ApiResult.success();
    }
}
