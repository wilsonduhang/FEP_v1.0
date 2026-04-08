package com.puchain.fep.web.submission.datasource.controller;

import com.puchain.fep.common.domain.ApiResult;
import com.puchain.fep.common.domain.PageResult;
import com.puchain.fep.web.submission.datasource.dto.DataSourceCreateRequest;
import com.puchain.fep.web.submission.datasource.dto.DataSourceResponse;
import com.puchain.fep.web.submission.datasource.service.SubDataSourceService;
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
 * 数据源管理 REST Controller。
 *
 * <p>提供数据源 CRUD 接口。
 * 参见 PRD v1.3 §5.5.3 数据源管理（FR-WEB-SUB-SRC）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/submission/data-sources")
@Tag(name = "数据源管理", description = "PRD §5.5.3 管理行内业务系统数据源配置")
public class SubDataSourceController {

    private final SubDataSourceService dataSourceService;

    /**
     * 构造 SubDataSourceController。
     *
     * @param dataSourceService 数据源管理服务
     */
    public SubDataSourceController(final SubDataSourceService dataSourceService) {
        this.dataSourceService = dataSourceService;
    }

    /**
     * 搜索数据源（分页）。
     *
     * @param keyword  关键字（可选，匹配数据源名称）
     * @param pageNum  页码（1-based，默认 1）
     * @param pageSize 每页大小（默认 10）
     * @return 分页数据源列表
     */
    @GetMapping
    @OperationLog(module = "数据源管理", type = OperationType.QUERY, description = "搜索数据源")
    @Operation(summary = "搜索数据源", description = "按数据源名称模糊搜索，分页返回")
    @ApiResponse(responseCode = "200", description = "搜索成功")
    public ApiResult<PageResult<DataSourceResponse>> search(
            @Parameter(description = "数据源名称关键字")
            @RequestParam(required = false) final String keyword,
            @Parameter(description = "页码（从1开始）")
            @RequestParam(defaultValue = "1") final int pageNum,
            @Parameter(description = "每页条数")
            @RequestParam(defaultValue = "10") final int pageSize) {
        return ApiResult.success(dataSourceService.search(keyword, pageNum, pageSize));
    }

    /**
     * 获取数据源详情。
     *
     * @param sourceId 数据源 ID
     * @return 数据源详情
     */
    @GetMapping("/{sourceId}")
    @OperationLog(module = "数据源管理", type = OperationType.QUERY, description = "查询数据源详情")
    @Operation(summary = "数据源详情", description = "按 ID 查询数据源")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public ApiResult<DataSourceResponse> getById(
            @Parameter(description = "数据源 ID") @PathVariable final String sourceId) {
        return ApiResult.success(dataSourceService.getById(sourceId));
    }

    /**
     * 新增数据源。
     *
     * @param request 创建请求
     * @return 新建数据源信息
     */
    @PostMapping
    @OperationLog(module = "数据源管理", type = OperationType.CREATE, description = "新增数据源")
    @Operation(summary = "新增数据源", description = "创建新的数据源配置")
    @ApiResponse(responseCode = "200", description = "创建成功")
    @ApiResponse(responseCode = "400", description = "参数校验失败")
    @ApiResponse(responseCode = "409", description = "名称已存在")
    public ApiResult<DataSourceResponse> create(
            @Valid @RequestBody final DataSourceCreateRequest request) {
        return ApiResult.success(dataSourceService.create(request));
    }

    /**
     * 编辑数据源。
     *
     * @param sourceId 数据源 ID
     * @param request  更新请求
     * @return 更新后的数据源信息
     */
    @PutMapping("/{sourceId}")
    @OperationLog(module = "数据源管理", type = OperationType.UPDATE, description = "编辑数据源")
    @Operation(summary = "编辑数据源", description = "修改数据源配置")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @ApiResponse(responseCode = "400", description = "参数校验失败")
    @ApiResponse(responseCode = "404", description = "数据源不存在")
    @ApiResponse(responseCode = "409", description = "名称已存在")
    public ApiResult<DataSourceResponse> update(
            @Parameter(description = "数据源 ID") @PathVariable final String sourceId,
            @Valid @RequestBody final DataSourceCreateRequest request) {
        return ApiResult.success(dataSourceService.update(sourceId, request));
    }

    /**
     * 删除数据源。
     *
     * @param sourceId 数据源 ID
     * @return 空响应
     */
    @DeleteMapping("/{sourceId}")
    @OperationLog(module = "数据源管理", type = OperationType.DELETE, description = "删除数据源")
    @Operation(summary = "删除数据源", description = "物理删除数据源")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @ApiResponse(responseCode = "404", description = "数据源不存在")
    public ApiResult<Void> delete(
            @Parameter(description = "数据源 ID") @PathVariable final String sourceId) {
        dataSourceService.delete(sourceId);
        return ApiResult.success();
    }
}
