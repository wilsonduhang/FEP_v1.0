package com.puchain.fep.web.submission.outputinterface.controller;

import com.puchain.fep.common.domain.ApiResult;
import com.puchain.fep.common.domain.PageResult;
import com.puchain.fep.web.submission.outputinterface.dto.OutputInterfaceCreateRequest;
import com.puchain.fep.web.submission.outputinterface.dto.OutputInterfaceResponse;
import com.puchain.fep.web.submission.outputinterface.service.SubOutputInterfaceService;
import com.puchain.fep.web.sysmgmt.log.annotation.OperationLog;
import com.puchain.fep.web.sysmgmt.log.domain.OperationType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 输出接口管理 REST Controller。
 *
 * <p>提供输出接口 CRUD、状态切换及连通性测试接口。
 * 参见 PRD v1.3 §5.5.2 输出接口管理（FR-WEB-SUB-OUT）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/submission/output-interfaces")
@Tag(name = "输出接口管理", description = "PRD §5.5.2 管理对行内业务系统输出的标准接口")
public class SubOutputInterfaceController {

    private final SubOutputInterfaceService outputInterfaceService;

    /**
     * 构造 SubOutputInterfaceController。
     *
     * @param outputInterfaceService 输出接口管理服务
     */
    public SubOutputInterfaceController(final SubOutputInterfaceService outputInterfaceService) {
        this.outputInterfaceService = outputInterfaceService;
    }

    /**
     * 搜索输出接口（分页）。
     *
     * @param keyword  关键字（可选，匹配接口名称）
     * @param pageNum  页码（1-based，默认 1）
     * @param pageSize 每页大小（默认 10）
     * @return 分页输出接口列表
     */
    @GetMapping
    @OperationLog(module = "输出接口管理", type = OperationType.QUERY, description = "搜索输出接口")
    @Operation(summary = "搜索输出接口", description = "按接口名称模糊搜索，分页返回")
    @ApiResponse(responseCode = "200", description = "搜索成功")
    public ApiResult<PageResult<OutputInterfaceResponse>> search(
            @Parameter(description = "接口名称关键字")
            @RequestParam(required = false) final String keyword,
            @Parameter(description = "页码（从1开始）")
            @RequestParam(defaultValue = "1") final int pageNum,
            @Parameter(description = "每页条数")
            @RequestParam(defaultValue = "10") final int pageSize) {
        return ApiResult.success(outputInterfaceService.search(keyword, pageNum, pageSize));
    }

    /**
     * 获取输出接口详情。
     *
     * @param interfaceId 接口 ID
     * @return 输出接口详情
     */
    @GetMapping("/{interfaceId}")
    @OperationLog(module = "输出接口管理", type = OperationType.QUERY, description = "查询输出接口详情")
    @Operation(summary = "输出接口详情", description = "按 ID 查询输出接口")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public ApiResult<OutputInterfaceResponse> getById(
            @Parameter(description = "接口 ID") @PathVariable final String interfaceId) {
        return ApiResult.success(outputInterfaceService.getById(interfaceId));
    }

    /**
     * 新增输出接口。
     *
     * @param request 创建请求
     * @return 新建输出接口信息
     */
    @PostMapping
    @OperationLog(module = "输出接口管理", type = OperationType.CREATE, description = "新增输出接口")
    @Operation(summary = "新增输出接口", description = "创建新的输出接口配置")
    @ApiResponse(responseCode = "200", description = "创建成功")
    @ApiResponse(responseCode = "400", description = "参数校验失败")
    @ApiResponse(responseCode = "409", description = "名称已存在")
    public ApiResult<OutputInterfaceResponse> create(
            @Valid @RequestBody final OutputInterfaceCreateRequest request) {
        return ApiResult.success(outputInterfaceService.create(request));
    }

    /**
     * 编辑输出接口。
     *
     * @param interfaceId 接口 ID
     * @param request     更新请求
     * @return 更新后的输出接口信息
     */
    @PutMapping("/{interfaceId}")
    @OperationLog(module = "输出接口管理", type = OperationType.UPDATE, description = "编辑输出接口")
    @Operation(summary = "编辑输出接口", description = "修改输出接口配置")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @ApiResponse(responseCode = "400", description = "参数校验失败")
    @ApiResponse(responseCode = "404", description = "接口不存在")
    @ApiResponse(responseCode = "409", description = "名称已存在")
    public ApiResult<OutputInterfaceResponse> update(
            @Parameter(description = "接口 ID") @PathVariable final String interfaceId,
            @Valid @RequestBody final OutputInterfaceCreateRequest request) {
        return ApiResult.success(outputInterfaceService.update(interfaceId, request));
    }

    /**
     * 切换输出接口状态（启用/停用）。
     *
     * @param interfaceId 接口 ID
     * @return 更新后的输出接口信息
     */
    @PatchMapping("/{interfaceId}/status")
    @OperationLog(module = "输出接口管理", type = OperationType.UPDATE,
            description = "切换输出接口状态")
    @Operation(summary = "启用/停用输出接口", description = "ENABLED↔DISABLED 切换")
    @ApiResponse(responseCode = "200", description = "切换成功")
    @ApiResponse(responseCode = "404", description = "接口不存在")
    public ApiResult<OutputInterfaceResponse> toggleStatus(
            @Parameter(description = "接口 ID") @PathVariable final String interfaceId) {
        return ApiResult.success(outputInterfaceService.toggleStatus(interfaceId));
    }

    /**
     * 删除输出接口。
     *
     * @param interfaceId 接口 ID
     * @return 空响应
     */
    @DeleteMapping("/{interfaceId}")
    @OperationLog(module = "输出接口管理", type = OperationType.DELETE, description = "删除输出接口")
    @Operation(summary = "删除输出接口", description = "物理删除输出接口")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @ApiResponse(responseCode = "404", description = "接口不存在")
    public ApiResult<Void> delete(
            @Parameter(description = "接口 ID") @PathVariable final String interfaceId) {
        outputInterfaceService.delete(interfaceId);
        return ApiResult.success();
    }

    /**
     * 测试输出接口连通性。
     *
     * @param interfaceId 接口 ID
     * @return 连通性结果（true=连通，false=不通）
     */
    @PostMapping("/{interfaceId}/test")
    @OperationLog(module = "输出接口管理", type = OperationType.QUERY,
            description = "测试输出接口连通性")
    @Operation(summary = "测试连通性", description = "HTTP HEAD 探测目标接口 URL")
    @ApiResponse(responseCode = "200", description = "测试完成")
    public ApiResult<Boolean> testConnectivity(
            @Parameter(description = "接口 ID") @PathVariable final String interfaceId) {
        return ApiResult.success(outputInterfaceService.testConnectivity(interfaceId));
    }
}
