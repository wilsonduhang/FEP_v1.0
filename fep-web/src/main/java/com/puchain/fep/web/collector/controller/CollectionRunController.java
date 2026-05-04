package com.puchain.fep.web.collector.controller;

import com.puchain.fep.common.domain.ApiResult;
import com.puchain.fep.common.domain.PageResult;
import com.puchain.fep.web.collector.dto.CollectionRunQueryRequest;
import com.puchain.fep.web.collector.dto.CollectionRunResponse;
import com.puchain.fep.web.collector.service.CollectionRunQueryService;
import com.puchain.fep.web.sysmgmt.log.annotation.OperationLog;
import com.puchain.fep.web.sysmgmt.log.domain.OperationType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * 数据采集运行历史 REST Controller（PRD §5.5 报送管理）。
 *
 * <p>P4 T6b — 单端点：{@code GET /api/v1/collector/runs}。分页查询采集运行历史，
 * 支持可选过滤：{@code adapterId} / {@code status} / {@code from} / {@code to}。</p>
 *
 * <h3>分页契约</h3>
 * <p>{@code pageNum} 从 {@code 1} 开始（项目惯例 — 见
 * {@link com.puchain.fep.common.domain.PageQuery}）；
 * service 层把 1-based pageNum 转为 Spring Data 0-based {@code Pageable}
 * （内联模式，不在 fep-common {@code PageQuery} 上扩 {@code toPageable()}
 * — 红线 {@code feedback_pagination_adapter}）。</p>
 *
 * <h3>权限</h3>
 * <p>不加 {@code @PreAuthorize}（与 {@link CollectorTriggerController} 一致），
 * 详见 trigger controller Javadoc。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/collector/runs")
@Tag(name = "数据采集 - 运行历史",
        description = "PRD §5.5 — 分页查询采集运行历史（adapter / status / 时间窗）")
public class CollectionRunController {

    private final CollectionRunQueryService queryService;

    /**
     * Constructs the controller.
     *
     * @param queryService read-side query service, non-null
     */
    public CollectionRunController(final CollectionRunQueryService queryService) {
        this.queryService = Objects.requireNonNull(queryService, "queryService");
    }

    /**
     * 分页查询采集运行历史（PRD §5.5）。
     *
     * <p>{@code adapterId} / {@code status} 精确匹配；{@code from} / {@code to}
     * 是 {@code startedAt} 的闭区间，可单独指定其一（开放区间）。</p>
     *
     * @param request 查询条件 + 分页参数（{@code pageNum} 1-based）
     * @return 分页结果
     */
    @GetMapping
    @OperationLog(module = "数据采集", type = OperationType.QUERY,
            description = "分页查询采集运行历史")
    @Operation(summary = "查询采集运行历史",
            description = "按 adapterId / status / 时间窗过滤分页查询")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @ApiResponse(responseCode = "400", description = "分页参数校验失败")
    public ApiResult<PageResult<CollectionRunResponse>> search(
            @Valid final CollectionRunQueryRequest request) {
        return ApiResult.success(queryService.search(request));
    }
}
