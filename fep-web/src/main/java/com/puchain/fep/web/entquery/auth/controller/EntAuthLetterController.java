package com.puchain.fep.web.entquery.auth.controller;

import com.puchain.fep.common.domain.ApiResult;
import com.puchain.fep.common.domain.PageResult;
import com.puchain.fep.web.entquery.auth.dto.AuthLetterCreateRequest;
import com.puchain.fep.web.entquery.auth.dto.AuthLetterResponse;
import com.puchain.fep.web.entquery.auth.service.EntAuthLetterService;
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
 * 授权书管理 REST API。
 *
 * <p>提供授权书 CRUD 及提交接口。
 * 参见 PRD v1.3 §5.4 企业信息查询管理（FR-WEB-ENT）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/ent-query/auth-letters")
@Tag(name = "21. 企业查询 — 授权书管理", description = "授权书 CRUD + 提交")
public class EntAuthLetterController {

    private final EntAuthLetterService letterService;

    /**
     * 构造 EntAuthLetterController。
     *
     * @param letterService 授权书管理服务
     */
    public EntAuthLetterController(final EntAuthLetterService letterService) {
        this.letterService = letterService;
    }

    /**
     * 搜索授权书（分页）。
     *
     * @param authType     授权书类型（可选，PAPER/ELECTRONIC）
     * @param letterStatus 授权书状态（可选，DRAFT/SUBMITTED/ACKNOWLEDGED/REJECTED）
     * @param keyword      关键字（可选，匹配被授权企业 USCI 或名称）
     * @param pageNum      页码（1-based，默认 1）
     * @param pageSize     每页大小（默认 10）
     * @return 分页授权书列表
     */
    @GetMapping
    @OperationLog(module = "授权书管理", type = OperationType.QUERY, description = "搜索授权书")
    @Operation(summary = "搜索授权书", description = "按授权书类型/状态/关键字分页搜索")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public ApiResult<PageResult<AuthLetterResponse>> search(
            @Parameter(description = "授权书类型") @RequestParam(required = false) final String authType,
            @Parameter(description = "授权书状态") @RequestParam(required = false) final String letterStatus,
            @Parameter(description = "关键字") @RequestParam(required = false) final String keyword,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") final int pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") final int pageSize) {
        return ApiResult.success(letterService.search(authType, letterStatus, keyword, pageNum, pageSize));
    }

    /**
     * 按 ID 查询授权书详情。
     *
     * @param letterId 授权书 ID
     * @return 授权书详情
     */
    @GetMapping("/{letterId}")
    @OperationLog(module = "授权书管理", type = OperationType.QUERY, description = "查询授权书详情")
    @Operation(summary = "授权书详情", description = "按 ID 查询授权书")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @ApiResponse(responseCode = "404", description = "授权书不存在")
    public ApiResult<AuthLetterResponse> getById(
            @Parameter(description = "授权书 ID") @PathVariable final String letterId) {
        return ApiResult.success(letterService.getById(letterId));
    }

    /**
     * 创建授权书。
     *
     * @param request 创建请求
     * @return 新建授权书信息
     */
    @PostMapping
    @OperationLog(module = "授权书管理", type = OperationType.CREATE, description = "创建授权书")
    @Operation(summary = "创建授权书", description = "新建授权书，初始状态为 DRAFT")
    @ApiResponse(responseCode = "200", description = "创建成功")
    @ApiResponse(responseCode = "400", description = "参数校验失败")
    @ApiResponse(responseCode = "404", description = "企业主体不存在")
    public ApiResult<AuthLetterResponse> create(
            @Valid @RequestBody final AuthLetterCreateRequest request) {
        return ApiResult.success(letterService.create(request));
    }

    /**
     * 更新授权书（仅 DRAFT 状态）。
     *
     * @param letterId 授权书 ID
     * @param request  更新请求
     * @return 更新后的授权书信息
     */
    @PutMapping("/{letterId}")
    @OperationLog(module = "授权书管理", type = OperationType.UPDATE, description = "更新授权书")
    @Operation(summary = "更新授权书", description = "仅 DRAFT 状态的授权书可更新")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @ApiResponse(responseCode = "400", description = "授权书状态不允许更新")
    @ApiResponse(responseCode = "404", description = "授权书不存在")
    public ApiResult<AuthLetterResponse> update(
            @Parameter(description = "授权书 ID") @PathVariable final String letterId,
            @Valid @RequestBody final AuthLetterCreateRequest request) {
        return ApiResult.success(letterService.update(letterId, request));
    }

    /**
     * 提交授权书（DRAFT -> SUBMITTED）。
     *
     * @param letterId 授权书 ID
     * @return 更新后的授权书信息
     */
    @PostMapping("/{letterId}/submit")
    @OperationLog(module = "授权书管理", type = OperationType.UPDATE, description = "提交授权书")
    @Operation(summary = "提交授权书", description = "将 DRAFT 状态授权书变更为 SUBMITTED")
    @ApiResponse(responseCode = "200", description = "提交成功")
    @ApiResponse(responseCode = "400", description = "授权书状态不允许提交")
    @ApiResponse(responseCode = "404", description = "授权书不存在")
    public ApiResult<AuthLetterResponse> submit(
            @Parameter(description = "授权书 ID") @PathVariable final String letterId) {
        return ApiResult.success(letterService.submit(letterId));
    }

    /**
     * 删除授权书（仅 DRAFT 状态）。
     *
     * @param letterId 授权书 ID
     * @return 空响应
     */
    @DeleteMapping("/{letterId}")
    @OperationLog(module = "授权书管理", type = OperationType.DELETE, description = "删除授权书")
    @Operation(summary = "删除授权书", description = "仅 DRAFT 状态的授权书可删除")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @ApiResponse(responseCode = "400", description = "授权书状态不允许删除")
    @ApiResponse(responseCode = "404", description = "授权书不存在")
    public ApiResult<Void> delete(
            @Parameter(description = "授权书 ID") @PathVariable final String letterId) {
        letterService.delete(letterId);
        return ApiResult.success();
    }
}
