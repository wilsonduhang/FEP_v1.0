package com.puchain.fep.web.sysmgmt.config.pushinterface.controller;

import com.puchain.fep.common.domain.ApiResult;
import com.puchain.fep.common.domain.EnableDisableStatus;
import com.puchain.fep.common.domain.PageResult;
import com.puchain.fep.web.sysmgmt.config.pushinterface.dto.PushInterfaceCreateRequest;
import com.puchain.fep.web.sysmgmt.config.pushinterface.dto.PushInterfaceResponse;
import com.puchain.fep.web.sysmgmt.config.pushinterface.service.SysPushInterfaceService;
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
 * 推送接口管理 REST API。
 *
 * <p>提供推送接口 CRUD 及状态切换接口。
 * 参见 PRD v1.3 §5.10.7.2c 推送接口管理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/sys/config/push-interfaces")
@Tag(name = "14. 推送接口管理", description = "推送接口 CRUD 及状态管理")
public class SysPushInterfaceController {

    private final SysPushInterfaceService pushInterfaceService;

    /**
     * 构造 SysPushInterfaceController。
     *
     * @param pushInterfaceService 推送接口管理服务
     */
    public SysPushInterfaceController(final SysPushInterfaceService pushInterfaceService) {
        this.pushInterfaceService = pushInterfaceService;
    }

    /**
     * 搜索推送接口（分页）。
     *
     * @param keyword  关键字（可选，匹配接口名称）
     * @param pageNum  页码（1-based，默认 1）
     * @param pageSize 每页大小（默认 20）
     * @return 分页推送接口列表
     */
    @GetMapping
    @OperationLog(module = "推送接口管理", type = OperationType.QUERY, description = "搜索推送接口")
    @Operation(summary = "搜索推送接口", description = "按接口名称关键字分页搜索")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public ApiResult<PageResult<PushInterfaceResponse>> search(
            @Parameter(description = "关键字") @RequestParam(required = false) final String keyword,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") final int pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") final int pageSize) {
        return ApiResult.success(pushInterfaceService.search(keyword, pageNum, pageSize));
    }

    /**
     * 创建推送接口。
     *
     * @param request 创建请求
     * @return 新建推送接口信息
     */
    @PostMapping
    @OperationLog(module = "推送接口管理", type = OperationType.CREATE, description = "创建推送接口")
    @Operation(summary = "创建推送接口", description = "新增推送接口，名称不可重复")
    @ApiResponse(responseCode = "200", description = "创建成功")
    @ApiResponse(responseCode = "400", description = "参数校验失败")
    @ApiResponse(responseCode = "409", description = "名称已存在")
    public ApiResult<PushInterfaceResponse> create(
            @Valid @RequestBody final PushInterfaceCreateRequest request) {
        return ApiResult.success(pushInterfaceService.create(request));
    }

    /**
     * 更新推送接口信息。
     *
     * @param interfaceId 接口 ID
     * @param request     更新请求
     * @return 更新后的推送接口信息
     */
    @PutMapping("/{interfaceId}")
    @OperationLog(module = "推送接口管理", type = OperationType.UPDATE, description = "更新推送接口")
    @Operation(summary = "更新推送接口", description = "修改推送接口名称、URL、推送方式、鉴权类型等信息")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @ApiResponse(responseCode = "400", description = "参数校验失败")
    @ApiResponse(responseCode = "404", description = "接口不存在")
    @ApiResponse(responseCode = "409", description = "名称已存在")
    public ApiResult<PushInterfaceResponse> update(
            @Parameter(description = "接口 ID") @PathVariable final String interfaceId,
            @Valid @RequestBody final PushInterfaceCreateRequest request) {
        return ApiResult.success(pushInterfaceService.update(interfaceId, request));
    }

    /**
     * 删除推送接口。
     *
     * @param interfaceId 接口 ID
     * @return 空响应
     */
    @DeleteMapping("/{interfaceId}")
    @OperationLog(module = "推送接口管理", type = OperationType.DELETE, description = "删除推送接口")
    @Operation(summary = "删除推送接口", description = "删除指定推送接口")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @ApiResponse(responseCode = "404", description = "接口不存在")
    public ApiResult<Void> delete(
            @Parameter(description = "接口 ID") @PathVariable final String interfaceId) {
        pushInterfaceService.delete(interfaceId);
        return ApiResult.success();
    }

    /**
     * 启用推送接口。
     *
     * @param interfaceId 接口 ID
     * @return 更新后的推送接口信息
     */
    @PostMapping("/{interfaceId}/enable")
    @OperationLog(module = "推送接口管理", type = OperationType.UPDATE, description = "启用推送接口")
    @Operation(summary = "启用推送接口", description = "将推送接口状态设置为 ENABLED")
    @ApiResponse(responseCode = "200", description = "启用成功")
    @ApiResponse(responseCode = "404", description = "接口不存在")
    public ApiResult<PushInterfaceResponse> enable(
            @Parameter(description = "接口 ID") @PathVariable final String interfaceId) {
        return ApiResult.success(
                pushInterfaceService.toggleStatus(interfaceId, EnableDisableStatus.ENABLED));
    }

    /**
     * 禁用推送接口。
     *
     * @param interfaceId 接口 ID
     * @return 更新后的推送接口信息
     */
    @PostMapping("/{interfaceId}/disable")
    @OperationLog(module = "推送接口管理", type = OperationType.UPDATE, description = "禁用推送接口")
    @Operation(summary = "禁用推送接口", description = "将推送接口状态设置为 DISABLED")
    @ApiResponse(responseCode = "200", description = "禁用成功")
    @ApiResponse(responseCode = "404", description = "接口不存在")
    public ApiResult<PushInterfaceResponse> disable(
            @Parameter(description = "接口 ID") @PathVariable final String interfaceId) {
        return ApiResult.success(
                pushInterfaceService.toggleStatus(interfaceId, EnableDisableStatus.DISABLED));
    }
}
