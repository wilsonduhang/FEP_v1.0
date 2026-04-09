package com.puchain.fep.web.tlq.connectivity.controller;

import com.puchain.fep.common.domain.ApiResult;
import com.puchain.fep.common.domain.PageResult;
import com.puchain.fep.web.sysmgmt.log.annotation.OperationLog;
import com.puchain.fep.web.sysmgmt.log.domain.OperationType;
import com.puchain.fep.web.tlq.connectivity.dto.ConnectivityRecordResponse;
import com.puchain.fep.web.tlq.connectivity.dto.ConnectivitySummaryResponse;
import com.puchain.fep.web.tlq.connectivity.dto.ConnectivityTestResponse;
import com.puchain.fep.web.tlq.connectivity.service.TlqConnectivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * TLQ 连通性测试 REST Controller。
 *
 * <p>提供 TLQ 9005 心跳测试触发、历史记录查询和统计汇总接口。
 * 参见 PRD v1.3 §5.7.5 TLQ 连通性监控（FR-WEB-TLQ-CONN）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/tlq/connectivity")
@Tag(name = "TLQ 连通性测试", description = "PRD §5.7.5 管理 9005 心跳测试和连通性监控")
public class TlqConnectivityController {

    private final TlqConnectivityService connectivityService;

    /**
     * 构造 TlqConnectivityController。
     *
     * @param connectivityService TLQ 连通性测试服务
     */
    public TlqConnectivityController(final TlqConnectivityService connectivityService) {
        this.connectivityService = connectivityService;
    }

    /**
     * 触发指定节点的连通性测试。
     *
     * <p>当前为占位符实现，实际 9005 心跳发送待 P1 TLQ SDK 就绪后接入。</p>
     *
     * @param nodeId 目标节点 ID
     * @return 本次测试结果
     */
    @PostMapping("/{nodeId}/test")
    @ResponseStatus(HttpStatus.CREATED)
    @OperationLog(module = "TLQ 连通性测试", type = OperationType.CREATE,
            description = "触发 TLQ 节点连通性测试")
    @Operation(summary = "触发连通性测试", description = "触发指定节点的 9005 心跳测试并记录结果")
    @ApiResponse(responseCode = "201", description = "测试已触发")
    @ApiResponse(responseCode = "404", description = "节点不存在")
    public ApiResult<ConnectivityTestResponse> triggerTest(
            @Parameter(description = "节点 ID") @PathVariable final String nodeId) {
        return ApiResult.success(connectivityService.triggerTest(nodeId));
    }

    /**
     * 分页查询指定节点的连通性历史记录（测试时间倒序）。
     *
     * @param nodeId 目标节点 ID
     * @param page   页码（0-based，默认 0）
     * @param size   每页大小（默认 20）
     * @return 分页历史记录
     */
    @GetMapping("/{nodeId}/records")
    @OperationLog(module = "TLQ 连通性测试", type = OperationType.QUERY,
            description = "查询节点连通性历史记录")
    @Operation(summary = "连通性历史记录", description = "分页查询指定节点的连通性测试历史，测试时间倒序")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @ApiResponse(responseCode = "404", description = "节点不存在")
    public ApiResult<PageResult<ConnectivityRecordResponse>> listRecords(
            @Parameter(description = "节点 ID") @PathVariable final String nodeId,
            @Parameter(description = "页码（从 0 开始）")
            @RequestParam(defaultValue = "0") final int page,
            @Parameter(description = "每页条数")
            @RequestParam(defaultValue = "20") final int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "testTime"));
        Page<ConnectivityRecordResponse> result = connectivityService.listRecords(nodeId, pageable);
        return ApiResult.success(new PageResult<>(
                result.getContent(),
                result.getTotalElements(),
                page + 1,
                size));
    }

    /**
     * 获取指定节点的连通性测试统计汇总。
     *
     * <p>包括最近测试结果、总测试次数、成功次数及成功率。</p>
     *
     * @param nodeId 目标节点 ID
     * @return 统计汇总响应
     */
    @GetMapping("/{nodeId}/summary")
    @OperationLog(module = "TLQ 连通性测试", type = OperationType.QUERY,
            description = "查询节点连通性统计汇总")
    @Operation(summary = "连通性统计汇总", description = "获取指定节点的连通性测试成功率及最近测试结果")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @ApiResponse(responseCode = "404", description = "节点不存在")
    public ApiResult<ConnectivitySummaryResponse> getSummary(
            @Parameter(description = "节点 ID") @PathVariable final String nodeId) {
        return ApiResult.success(connectivityService.getSummary(nodeId));
    }
}
