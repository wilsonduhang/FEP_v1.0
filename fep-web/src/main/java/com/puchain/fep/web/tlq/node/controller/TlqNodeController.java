package com.puchain.fep.web.tlq.node.controller;

import com.puchain.fep.common.domain.ApiResult;
import com.puchain.fep.common.domain.PageResult;
import com.puchain.fep.web.sysmgmt.log.annotation.OperationLog;
import com.puchain.fep.web.sysmgmt.log.domain.OperationType;
import com.puchain.fep.web.tlq.node.domain.TlqNodeRole;
import com.puchain.fep.web.tlq.node.domain.TlqNodeStatus;
import com.puchain.fep.web.tlq.node.dto.TlqNodeCreateRequest;
import com.puchain.fep.web.tlq.node.dto.TlqNodeResponse;
import com.puchain.fep.web.tlq.node.dto.TlqNodeUpdateRequest;
import com.puchain.fep.web.tlq.node.service.TlqNodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * TLQ 节点管理 REST Controller。
 *
 * <p>提供 TLQ 节点 CRUD、状态切换接口。
 * 参见 PRD v1.3 §5.7 TLQ 节点管理（FR-WEB-TLQ-NODE）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/tlq/nodes")
@Tag(name = "TLQ 节点管理", description = "PRD §5.7 管理 TLQ 双主双从架构中的节点配置与状态")
public class TlqNodeController {

    private final TlqNodeService tlqNodeService;

    /**
     * 构造 TlqNodeController。
     *
     * @param tlqNodeService TLQ 节点管理服务
     */
    public TlqNodeController(final TlqNodeService tlqNodeService) {
        this.tlqNodeService = tlqNodeService;
    }

    /**
     * 新建 TLQ 节点。
     *
     * @param request 创建请求
     * @return 新建节点信息
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @OperationLog(module = "TLQ 节点管理", type = OperationType.CREATE, description = "新建 TLQ 节点")
    @Operation(summary = "新建 TLQ 节点", description = "创建 TLQ 双主双从节点配置，初始状态 UNKNOWN")
    @ApiResponse(responseCode = "201", description = "创建成功")
    @ApiResponse(responseCode = "400", description = "参数校验失败")
    @ApiResponse(responseCode = "409", description = "节点名称或 IP+端口已存在")
    public ApiResult<TlqNodeResponse> createNode(
            @Valid @RequestBody final TlqNodeCreateRequest request) {
        return ApiResult.success(tlqNodeService.createNode(request));
    }

    /**
     * 查询 TLQ 节点详情。
     *
     * @param nodeId 节点 ID
     * @return 节点详情
     */
    @GetMapping("/{nodeId}")
    @OperationLog(module = "TLQ 节点管理", type = OperationType.QUERY, description = "查询 TLQ 节点详情")
    @Operation(summary = "TLQ 节点详情", description = "按 ID 查询 TLQ 节点")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @ApiResponse(responseCode = "404", description = "节点不存在")
    public ApiResult<TlqNodeResponse> getNode(
            @Parameter(description = "节点 ID") @PathVariable final String nodeId) {
        return ApiResult.success(tlqNodeService.getNode(nodeId));
    }

    /**
     * 分页查询 TLQ 节点列表，可按角色/状态过滤。
     *
     * @param role   节点角色（可选）
     * @param status 节点状态（可选）
     * @param page   页码（0-based，默认 0）
     * @param size   每页大小（默认 20）
     * @return 分页节点列表
     */
    @GetMapping
    @OperationLog(module = "TLQ 节点管理", type = OperationType.QUERY, description = "查询 TLQ 节点列表")
    @Operation(summary = "TLQ 节点列表", description = "分页查询 TLQ 节点，可按角色和状态过滤")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public ApiResult<PageResult<TlqNodeResponse>> listNodes(
            @Parameter(description = "节点角色过滤（可选）")
            @RequestParam(required = false) final TlqNodeRole role,
            @Parameter(description = "节点状态过滤（可选）")
            @RequestParam(required = false) final TlqNodeStatus status,
            @Parameter(description = "页码（从 0 开始）")
            @RequestParam(defaultValue = "0") final int page,
            @Parameter(description = "每页条数")
            @RequestParam(defaultValue = "20") final int size) {
        return ApiResult.success(tlqNodeService.listNodes(role, status, page + 1, size));
    }

    /**
     * 更新 TLQ 节点（角色不可修改）。
     *
     * @param nodeId  节点 ID
     * @param request 更新请求
     * @return 更新后节点信息
     */
    @PutMapping("/{nodeId}")
    @OperationLog(module = "TLQ 节点管理", type = OperationType.UPDATE, description = "更新 TLQ 节点")
    @Operation(summary = "更新 TLQ 节点", description = "Partial update，节点角色不可修改")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @ApiResponse(responseCode = "400", description = "参数校验失败")
    @ApiResponse(responseCode = "404", description = "节点不存在")
    @ApiResponse(responseCode = "409", description = "节点名称或 IP+端口已存在")
    public ApiResult<TlqNodeResponse> updateNode(
            @Parameter(description = "节点 ID") @PathVariable final String nodeId,
            @Valid @RequestBody final TlqNodeUpdateRequest request) {
        return ApiResult.success(tlqNodeService.updateNode(nodeId, request));
    }

    /**
     * 删除 TLQ 节点（存在关联队列时拒绝）。
     *
     * @param nodeId 节点 ID
     * @return 空响应
     */
    @DeleteMapping("/{nodeId}")
    @OperationLog(module = "TLQ 节点管理", type = OperationType.DELETE, description = "删除 TLQ 节点")
    @Operation(summary = "删除 TLQ 节点", description = "物理删除节点，存在关联队列时拒绝")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @ApiResponse(responseCode = "404", description = "节点不存在")
    @ApiResponse(responseCode = "409", description = "存在关联队列，无法删除")
    public ApiResult<Void> deleteNode(
            @Parameter(description = "节点 ID") @PathVariable final String nodeId) {
        tlqNodeService.deleteNode(nodeId);
        return ApiResult.success();
    }

    /**
     * 变更 TLQ 节点状态。
     *
     * <p>状态机：UNKNOWN→ONLINE→OFFLINE→ONLINE，非法迁移返回 409。</p>
     *
     * @param nodeId 节点 ID
     * @param target 目标状态
     * @return 更新后节点信息
     */
    @PatchMapping("/{nodeId}/status")
    @OperationLog(module = "TLQ 节点管理", type = OperationType.UPDATE, description = "变更 TLQ 节点状态")
    @Operation(summary = "变更节点状态", description = "UNKNOWN→ONLINE→OFFLINE→ONLINE 状态流转")
    @ApiResponse(responseCode = "200", description = "切换成功")
    @ApiResponse(responseCode = "404", description = "节点不存在")
    @ApiResponse(responseCode = "409", description = "非法状态迁移")
    public ApiResult<TlqNodeResponse> changeStatus(
            @Parameter(description = "节点 ID") @PathVariable final String nodeId,
            @Parameter(description = "目标状态") @RequestParam final TlqNodeStatus target) {
        return ApiResult.success(tlqNodeService.changeStatus(nodeId, target));
    }
}
