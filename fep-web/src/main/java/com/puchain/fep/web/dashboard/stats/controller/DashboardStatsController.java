package com.puchain.fep.web.dashboard.stats.controller;

import com.puchain.fep.common.domain.ApiResult;
import com.puchain.fep.web.dashboard.stats.dto.DistributionItem;
import com.puchain.fep.web.dashboard.stats.dto.StatsCardsResponse;
import com.puchain.fep.web.dashboard.stats.dto.StatusDistributionItem;
import com.puchain.fep.web.dashboard.stats.dto.TimeRange;
import com.puchain.fep.web.dashboard.stats.dto.TrendDataPoint;
import com.puchain.fep.web.dashboard.stats.service.DashboardStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Dashboard statistics REST controller.
 *
 * <p>Provides read-only endpoints for the home page data statistics
 * dashboard. No {@code @OperationLog} — all operations are read-only.
 * See PRD v1.3 section 5.2.3 (FR-WEB-DASH-STAT).</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/dashboard/stats")
@Tag(name = "首页数据统计看板",
        description = "PRD section 5.2.3 数据统计看板")
public class DashboardStatsController {

    private final DashboardStatsService statsService;

    /**
     * Constructs DashboardStatsController.
     *
     * @param statsService dashboard statistics service
     */
    public DashboardStatsController(
            final DashboardStatsService statsService) {
        this.statsService = statsService;
    }

    /**
     * Returns summary card statistics.
     *
     * @return four card values: totalAmount, successCount,
     *         todayMessageCount, exceptionCount
     */
    @GetMapping("/cards")
    @Operation(summary = "统计卡片",
            description = "返回累计交易额/成功笔数/今日报文量/异常数")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public ApiResult<StatsCardsResponse> getCards() {
        return ApiResult.success(statsService.getCards());
    }

    /**
     * Returns trend data for the specified time range.
     *
     * @param range time range (defaults to TODAY)
     * @param start custom start date (for CUSTOM range)
     * @param end   custom end date (for CUSTOM range)
     * @return list of trend data points
     */
    @GetMapping("/trend")
    @Operation(summary = "趋势数据",
            description = "按时间范围返回发送/接收趋势")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public ApiResult<List<TrendDataPoint>> getTrend(
            @Parameter(description = "时间范围")
            @RequestParam(defaultValue = "TODAY")
            final TimeRange range,
            @Parameter(description = "自定义开始日期 (CUSTOM)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            final LocalDate start,
            @Parameter(description = "自定义结束日期 (CUSTOM)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            final LocalDate end) {
        return ApiResult.success(
                statsService.getTrend(range, start, end));
    }

    /**
     * Returns message distribution by business type (message code).
     *
     * @return list of distribution items with percentage
     */
    @GetMapping("/distribution")
    @Operation(summary = "业务类型分布",
            description = "按报文类型统计分布及占比")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public ApiResult<List<DistributionItem>> getDistribution() {
        return ApiResult.success(statsService.getDistribution());
    }

    /**
     * Returns message distribution by processing status.
     *
     * @return list of status distribution items with percentage
     */
    @GetMapping("/status-distribution")
    @Operation(summary = "处理状态分布",
            description = "按处理状态统计占比")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public ApiResult<List<StatusDistributionItem>>
            getStatusDistribution() {
        return ApiResult.success(
                statsService.getStatusDistribution());
    }
}
