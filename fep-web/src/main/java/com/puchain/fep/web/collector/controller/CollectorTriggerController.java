package com.puchain.fep.web.collector.controller;

import com.puchain.fep.collector.scheduler.CollectorScheduler;
import com.puchain.fep.collector.support.CollectionRunResult;
import com.puchain.fep.common.domain.ApiResult;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.web.collector.dto.CollectorTriggerRequest;
import com.puchain.fep.web.collector.dto.CollectorTriggerResponse;
import com.puchain.fep.web.sysmgmt.log.annotation.OperationLog;
import com.puchain.fep.web.sysmgmt.log.domain.OperationType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * 数据采集手动触发 REST Controller（PRD §2.2.3 + §5.5 报送管理）。
 *
 * <p>P4 T6b — 单端点：{@code POST /api/v1/collector/triggers}。委托
 * {@link CollectorScheduler#triggerManually(String)} 完成实际触发，错误码
 * {@link com.puchain.fep.common.domain.FepErrorCode#COLLECT_TRIGGER_REJECTED}
 * 由 {@link com.puchain.fep.common.exception.GlobalExceptionHandler} 统一映射 HTTP 400。</p>
 *
 * <h3>权限</h3>
 * <p>不加 {@code @PreAuthorize}（与 {@link com.puchain.fep.web.reconciliation.controller.ReconciliationController}
 * 一致）。后续 RBAC 权限点 {@code report:collector:trigger} 由统一的方法级 security
 * 启用环节接入（tracking ticket — P6d 权限对齐 / Plan §T6 #6）。</p>
 *
 * <h3>SKIPPED 语义</h3>
 * <p>当 {@link com.puchain.fep.collector.support.DistributedLock} 锁忙时
 * scheduler 返回 {@code Status.SKIPPED}（{@code runId=null}）。这是正常业务结果
 * （HTTP 200），由前端按 status 字段决定如何渲染。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/collector/triggers")
@Tag(name = "数据采集 - 手动触发",
        description = "PRD §2.2.3 + §5.5 — 手动启动指定 adapter 的一次采集运行")
public class CollectorTriggerController {

    private final CollectorScheduler scheduler;

    /**
     * Constructs the controller.
     *
     * @param scheduler the collector scheduler bean, non-null
     */
    public CollectorTriggerController(final CollectorScheduler scheduler) {
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
    }

    /**
     * 手动触发指定 adapter 的一次数据采集运行（PRD §2.2.3）。
     *
     * <p>响应状态对应：
     * <ul>
     *   <li>{@code SUCCESS / PARTIAL / FAILED} — adapter 启动并完成（FAILED 仍是业务结果，HTTP 200）</li>
     *   <li>{@code SKIPPED} — 分布式锁忙，未启动（正常情况，HTTP 200）</li>
     * </ul>
     * 当 adapterId 缺失或 adapter 已禁用时 {@link CollectorScheduler#triggerManually(String)}
     * 抛 {@link FepBusinessException}，由全局异常处理器映射 HTTP 400。
     *
     * @param request 触发请求
     * @return 包含运行结果的 {@link ApiResult}
     */
    @PostMapping
    @OperationLog(module = "数据采集", type = OperationType.CREATE,
            description = "手动触发数据采集运行")
    @Operation(summary = "手动触发数据采集",
            description = "调度指定 adapter 立即执行一次采集 + 组装 + 入队")
    @ApiResponse(responseCode = "200", description = "触发成功（含 SKIPPED 正常路径）")
    @ApiResponse(responseCode = "400",
            description = "adapterId 校验失败 / adapter 不存在或已禁用")
    public ApiResult<CollectorTriggerResponse> trigger(
            @Valid @RequestBody final CollectorTriggerRequest request) {
        final CollectionRunResult result = scheduler.triggerManually(request.getAdapterId());
        return ApiResult.success(CollectorTriggerResponse.from(result));
    }
}
