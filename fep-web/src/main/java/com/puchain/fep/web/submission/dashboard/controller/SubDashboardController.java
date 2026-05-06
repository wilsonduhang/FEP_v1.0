package com.puchain.fep.web.submission.dashboard.controller;

import com.puchain.fep.common.domain.ApiResult;
import com.puchain.fep.web.submission.dashboard.dto.DashboardDistributionItem;
import com.puchain.fep.web.submission.dashboard.dto.DashboardResponse;
import com.puchain.fep.web.submission.dashboard.dto.DashboardTrendResponse;
import com.puchain.fep.web.submission.dashboard.service.SubDashboardService;
import com.puchain.fep.web.sysmgmt.log.annotation.OperationLog;
import com.puchain.fep.web.sysmgmt.log.domain.OperationType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Submission dashboard REST Controller.
 *
 * <p>Provides aggregated statistics for the submission management overview page.
 * See PRD v1.3 section 5.5.1 Data Overview (FR-WEB-SUB-DASH).</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/submission/dashboard")
@Tag(name = "报送管理数据概况", description = "PRD §5.5.1 报送管理数据概况统计")
public class SubDashboardController {

    private final SubDashboardService dashboardService;

    /**
     * Constructs SubDashboardController.
     *
     * @param dashboardService dashboard statistics service
     */
    public SubDashboardController(final SubDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * Returns aggregated dashboard statistics.
     *
     * @return dashboard statistics including interface, data source, and record counts
     */
    @GetMapping
    @OperationLog(module = "报送管理数据概况", type = OperationType.QUERY,
            description = "查询报送管理数据概况")
    @Operation(summary = "数据概况统计", description = "汇总接口/数据源/报送记录数量")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public ApiResult<DashboardResponse> getDashboard() {
        return ApiResult.success(dashboardService.getDashboard());
    }

    /**
     * Returns push trend time-series grouped by day.
     *
     * @param days lookback window in days; must be 7 or 30
     * @return pushed / pending daily count series aligned with a date series
     */
    @GetMapping("/trend")
    @OperationLog(module = "报送管理数据概况", type = OperationType.QUERY,
            description = "查询推送趋势")
    @Operation(summary = "推送趋势", description = "按日粒度聚合 pushed/pending 数量")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @ApiResponse(responseCode = "400", description = "days 参数非 7/30")
    public ApiResult<DashboardTrendResponse> getTrend(
            @RequestParam(defaultValue = "7") final int days) {
        return ApiResult.success(dashboardService.getTrend(days));
    }

    /**
     * Returns Top-10 distribution grouped by the given dimension, optionally
     * scoped by a lookback window.
     *
     * @param dim  grouping dimension; must be {@code messageType} or {@code businessType}
     * @param days optional lookback window in days (1..365); defaults to 90 when omitted
     *             (PRD v1.3 §5.5.1 D-2)
     * @return Top 10 distribution items sorted by count descending
     */
    @GetMapping("/distribution")
    @OperationLog(module = "报送管理数据概况", type = OperationType.QUERY,
            description = "查询分布统计")
    @Operation(summary = "分布统计",
            description = "按 messageType/businessType 分组 Top 10，可选 days 限定时间窗（默认 90）")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @ApiResponse(responseCode = "400", description = "dim 非法 / days 越界（1..365）")
    public ApiResult<List<DashboardDistributionItem>> getDistribution(
            @Parameter(description = "分组维度，messageType 或 businessType", required = true)
            @RequestParam final String dim,
            @Parameter(description = "回看天数（1..365），默认 90")
            @RequestParam(required = false) final Integer days) {
        return ApiResult.success(dashboardService.getDistribution(dim, days));
    }
}
