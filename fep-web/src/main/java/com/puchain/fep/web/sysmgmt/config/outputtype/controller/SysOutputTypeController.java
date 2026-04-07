package com.puchain.fep.web.sysmgmt.config.outputtype.controller;

import com.puchain.fep.common.domain.ApiResult;
import com.puchain.fep.common.domain.PageResult;
import com.puchain.fep.web.sysmgmt.config.outputtype.dto.OutputTypeCreateRequest;
import com.puchain.fep.web.sysmgmt.config.outputtype.dto.OutputTypeResponse;
import com.puchain.fep.web.sysmgmt.config.outputtype.service.SysOutputTypeService;
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
 * 输出类型管理 REST API。
 *
 * <p>提供输出类型 CRUD 接口。
 * 参见 PRD v1.3 §5.10.7.2e 输出类型管理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/sys/config/output-types")
@Tag(name = "11. 输出类型管理", description = "输出类型 CRUD")
public class SysOutputTypeController {

    private final SysOutputTypeService outputTypeService;

    /**
     * 构造 SysOutputTypeController。
     *
     * @param outputTypeService 输出类型管理服务
     */
    public SysOutputTypeController(final SysOutputTypeService outputTypeService) {
        this.outputTypeService = outputTypeService;
    }

    /**
     * 搜索输出类型（分页）。
     *
     * @param keyword  关键字（可选，匹配类型名称）
     * @param pageNum  页码（1-based，默认 1）
     * @param pageSize 每页大小（默认 20）
     * @return 分页输出类型列表
     */
    @GetMapping
    @OperationLog(module = "输出类型管理", type = OperationType.QUERY, description = "搜索输出类型")
    @Operation(summary = "搜索输出类型", description = "按类型名称关键字分页搜索")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public ApiResult<PageResult<OutputTypeResponse>> search(
            @Parameter(description = "关键字") @RequestParam(required = false) final String keyword,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") final int pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") final int pageSize) {
        return ApiResult.success(outputTypeService.search(keyword, pageNum, pageSize));
    }

    /**
     * 创建输出类型。
     *
     * @param request 创建请求
     * @return 新建输出类型信息
     */
    @PostMapping
    @OperationLog(module = "输出类型管理", type = OperationType.CREATE, description = "创建输出类型")
    @Operation(summary = "创建输出类型", description = "新增输出类型，编码不可重复")
    @ApiResponse(responseCode = "200", description = "创建成功")
    @ApiResponse(responseCode = "400", description = "参数校验失败")
    @ApiResponse(responseCode = "409", description = "编码已存在")
    public ApiResult<OutputTypeResponse> create(
            @Valid @RequestBody final OutputTypeCreateRequest request) {
        return ApiResult.success(outputTypeService.create(request));
    }

    /**
     * 更新输出类型信息。
     *
     * @param outputTypeId 输出类型 ID
     * @param request      更新请求
     * @return 更新后的输出类型信息
     */
    @PutMapping("/{outputTypeId}")
    @OperationLog(module = "输出类型管理", type = OperationType.UPDATE, description = "更新输出类型")
    @Operation(summary = "更新输出类型", description = "修改输出类型名称、编码")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @ApiResponse(responseCode = "404", description = "输出类型不存在")
    @ApiResponse(responseCode = "409", description = "编码已存在")
    public ApiResult<OutputTypeResponse> update(
            @Parameter(description = "输出类型 ID") @PathVariable final String outputTypeId,
            @Valid @RequestBody final OutputTypeCreateRequest request) {
        return ApiResult.success(outputTypeService.update(outputTypeId, request));
    }

    /**
     * 删除输出类型。
     *
     * @param outputTypeId 输出类型 ID
     * @return 空响应
     */
    @DeleteMapping("/{outputTypeId}")
    @OperationLog(module = "输出类型管理", type = OperationType.DELETE, description = "删除输出类型")
    @Operation(summary = "删除输出类型", description = "删除指定输出类型")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @ApiResponse(responseCode = "404", description = "输出类型不存在")
    public ApiResult<Void> delete(
            @Parameter(description = "输出类型 ID") @PathVariable final String outputTypeId) {
        outputTypeService.delete(outputTypeId);
        return ApiResult.success();
    }
}
