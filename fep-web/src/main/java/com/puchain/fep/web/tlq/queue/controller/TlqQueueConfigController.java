package com.puchain.fep.web.tlq.queue.controller;

import com.puchain.fep.common.domain.ApiResult;
import com.puchain.fep.web.sysmgmt.log.annotation.OperationLog;
import com.puchain.fep.web.sysmgmt.log.domain.OperationType;
import com.puchain.fep.web.tlq.queue.dto.TlqQueueBatchGenerateRequest;
import com.puchain.fep.web.tlq.queue.dto.TlqQueueConfigCreateRequest;
import com.puchain.fep.web.tlq.queue.dto.TlqQueueConfigResponse;
import com.puchain.fep.web.tlq.queue.service.TlqQueueConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * TLQ 队列配置 REST Controller。
 *
 * <p>提供 TLQ 队列配置 CRUD 及批量生成标准队列接口。
 * 参见 PRD v1.3 §3.1.2 TLQ 队列命名和通道配置（FR-WEB-TLQ-QUEUE）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/tlq/queues")
@Tag(name = "TLQ 队列配置", description = "PRD §3.1.2 管理 TLQ 队列命名和通道配置")
public class TlqQueueConfigController {

    private final TlqQueueConfigService queueConfigService;

    /**
     * 构造 TlqQueueConfigController。
     *
     * @param queueConfigService TLQ 队列配置管理服务
     */
    public TlqQueueConfigController(final TlqQueueConfigService queueConfigService) {
        this.queueConfigService = queueConfigService;
    }

    /**
     * 新建单条队列配置。
     *
     * @param request 创建请求
     * @return 新建队列配置信息
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @OperationLog(module = "TLQ 队列配置", type = OperationType.CREATE, description = "新建 TLQ 队列配置")
    @Operation(summary = "新建队列配置", description = "创建单条 TLQ 队列配置")
    @ApiResponse(responseCode = "201", description = "创建成功")
    @ApiResponse(responseCode = "400", description = "参数校验失败")
    @ApiResponse(responseCode = "404", description = "所属节点不存在")
    @ApiResponse(responseCode = "409", description = "队列名称已存在")
    public ApiResult<TlqQueueConfigResponse> createQueue(
            @Valid @RequestBody final TlqQueueConfigCreateRequest request) {
        return ApiResult.success(queueConfigService.createQueue(request));
    }

    /**
     * 按 PRD §3.1.2 命名规范为指定节点批量生成 9 条标准队列配置。
     *
     * <p>已存在的队列名称自动跳过，不计入返回列表。</p>
     *
     * @param request 批量生成请求
     * @return 实际创建的队列配置列表
     */
    @PostMapping("/batch-generate")
    @ResponseStatus(HttpStatus.CREATED)
    @OperationLog(module = "TLQ 队列配置", type = OperationType.CREATE,
            description = "批量生成标准队列配置")
    @Operation(summary = "批量生成标准队列", description = "按 PRD §3.1.2 命名规范生成 9 条标准队列配置")
    @ApiResponse(responseCode = "201", description = "生成成功（已存在的跳过）")
    @ApiResponse(responseCode = "400", description = "参数校验失败")
    @ApiResponse(responseCode = "404", description = "所属节点不存在")
    public ApiResult<List<TlqQueueConfigResponse>> batchGenerate(
            @Valid @RequestBody final TlqQueueBatchGenerateRequest request) {
        return ApiResult.success(queueConfigService.batchGenerateQueues(request));
    }

    /**
     * 按节点 ID 查询队列配置列表。
     *
     * @param nodeId 节点 ID
     * @return 队列配置列表
     */
    @GetMapping
    @OperationLog(module = "TLQ 队列配置", type = OperationType.QUERY,
            description = "查询节点队列配置列表")
    @Operation(summary = "节点队列配置列表", description = "按节点 ID 查询所有队列配置")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @ApiResponse(responseCode = "404", description = "节点不存在")
    public ApiResult<List<TlqQueueConfigResponse>> listByNode(
            @Parameter(description = "节点 ID") @RequestParam final String nodeId) {
        return ApiResult.success(queueConfigService.listByNode(nodeId));
    }

    /**
     * 删除队列配置。
     *
     * @param queueId 队列 ID
     * @return 空响应
     */
    @DeleteMapping("/{queueId}")
    @OperationLog(module = "TLQ 队列配置", type = OperationType.DELETE, description = "删除 TLQ 队列配置")
    @Operation(summary = "删除队列配置", description = "物理删除指定队列配置")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @ApiResponse(responseCode = "404", description = "队列不存在")
    public ApiResult<Void> deleteQueue(
            @Parameter(description = "队列 ID") @PathVariable final String queueId) {
        queueConfigService.deleteQueue(queueId);
        return ApiResult.success();
    }
}
