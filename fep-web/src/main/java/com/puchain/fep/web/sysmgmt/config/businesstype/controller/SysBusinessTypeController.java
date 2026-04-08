package com.puchain.fep.web.sysmgmt.config.businesstype.controller;

import com.puchain.fep.common.domain.ApiResult;
import com.puchain.fep.common.domain.PageResult;
import com.puchain.fep.common.domain.EnableDisableStatus;
import com.puchain.fep.web.sysmgmt.config.businesstype.dto.BusinessTypeCreateRequest;
import com.puchain.fep.web.sysmgmt.config.businesstype.dto.BusinessTypeResponse;
import com.puchain.fep.web.sysmgmt.config.businesstype.service.SysBusinessTypeService;
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
 * 业务类型管理 REST API。
 *
 * <p>提供业务类型 CRUD、启用/停用接口。
 * 参见 PRD v1.3 §5.10.7.2a 业务类型管理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/sys/config/business-types")
@Tag(name = "10. 业务类型管理", description = "业务类型 CRUD / 启用停用")
public class SysBusinessTypeController {

    private final SysBusinessTypeService businessTypeService;

    /**
     * 构造 SysBusinessTypeController。
     *
     * @param businessTypeService 业务类型管理服务
     */
    public SysBusinessTypeController(final SysBusinessTypeService businessTypeService) {
        this.businessTypeService = businessTypeService;
    }

    /**
     * 搜索业务类型（分页）。
     *
     * @param keyword  关键字（可选，匹配类型名称）
     * @param pageNum  页码（1-based，默认 1）
     * @param pageSize 每页大小（默认 20）
     * @return 分页业务类型列表
     */
    @GetMapping
    @OperationLog(module = "业务类型管理", type = OperationType.QUERY, description = "搜索业务类型")
    @Operation(summary = "搜索业务类型", description = "按类型名称关键字分页搜索")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public ApiResult<PageResult<BusinessTypeResponse>> search(
            @Parameter(description = "关键字") @RequestParam(required = false) final String keyword,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") final int pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") final int pageSize) {
        return ApiResult.success(businessTypeService.search(keyword, pageNum, pageSize));
    }

    /**
     * 创建业务类型。
     *
     * @param request 创建请求
     * @return 新建业务类型信息
     */
    @PostMapping
    @OperationLog(module = "业务类型管理", type = OperationType.CREATE, description = "创建业务类型")
    @Operation(summary = "创建业务类型", description = "新增业务类型，编码不可重复")
    @ApiResponse(responseCode = "200", description = "创建成功")
    @ApiResponse(responseCode = "400", description = "参数校验失败")
    @ApiResponse(responseCode = "409", description = "编码已存在")
    public ApiResult<BusinessTypeResponse> create(
            @Valid @RequestBody final BusinessTypeCreateRequest request) {
        return ApiResult.success(businessTypeService.create(request));
    }

    /**
     * 更新业务类型信息。
     *
     * @param typeId  业务类型 ID
     * @param request 更新请求
     * @return 更新后的业务类型信息
     */
    @PutMapping("/{typeId}")
    @OperationLog(module = "业务类型管理", type = OperationType.UPDATE, description = "更新业务类型")
    @Operation(summary = "更新业务类型", description = "修改业务类型名称、编码、排序")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @ApiResponse(responseCode = "404", description = "业务类型不存在")
    @ApiResponse(responseCode = "409", description = "编码已存在")
    public ApiResult<BusinessTypeResponse> update(
            @Parameter(description = "业务类型 ID") @PathVariable final String typeId,
            @Valid @RequestBody final BusinessTypeCreateRequest request) {
        return ApiResult.success(businessTypeService.update(typeId, request));
    }

    /**
     * 删除业务类型。
     *
     * @param typeId 业务类型 ID
     * @return 空响应
     */
    @DeleteMapping("/{typeId}")
    @OperationLog(module = "业务类型管理", type = OperationType.DELETE, description = "删除业务类型")
    @Operation(summary = "删除业务类型", description = "删除指定业务类型")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @ApiResponse(responseCode = "404", description = "业务类型不存在")
    public ApiResult<Void> delete(
            @Parameter(description = "业务类型 ID") @PathVariable final String typeId) {
        businessTypeService.delete(typeId);
        return ApiResult.success();
    }

    /**
     * 启用业务类型。
     *
     * @param typeId 业务类型 ID
     * @return 更新后的业务类型信息
     */
    @PostMapping("/{typeId}/enable")
    @OperationLog(module = "业务类型管理", type = OperationType.UPDATE, description = "启用业务类型")
    @Operation(summary = "启用业务类型", description = "将业务类型状态设为 ENABLED")
    @ApiResponse(responseCode = "200", description = "启用成功")
    @ApiResponse(responseCode = "404", description = "业务类型不存在")
    public ApiResult<BusinessTypeResponse> enable(
            @Parameter(description = "业务类型 ID") @PathVariable final String typeId) {
        return ApiResult.success(businessTypeService.toggleStatus(typeId, EnableDisableStatus.ENABLED));
    }

    /**
     * 停用业务类型。
     *
     * @param typeId 业务类型 ID
     * @return 更新后的业务类型信息
     */
    @PostMapping("/{typeId}/disable")
    @OperationLog(module = "业务类型管理", type = OperationType.UPDATE, description = "停用业务类型")
    @Operation(summary = "停用业务类型", description = "将业务类型状态设为 DISABLED")
    @ApiResponse(responseCode = "200", description = "停用成功")
    @ApiResponse(responseCode = "404", description = "业务类型不存在")
    public ApiResult<BusinessTypeResponse> disable(
            @Parameter(description = "业务类型 ID") @PathVariable final String typeId) {
        return ApiResult.success(businessTypeService.toggleStatus(typeId, EnableDisableStatus.DISABLED));
    }
}
