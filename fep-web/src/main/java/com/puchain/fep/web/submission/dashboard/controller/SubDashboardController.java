package com.puchain.fep.web.submission.dashboard.controller;

import com.puchain.fep.common.domain.ApiResult;
import com.puchain.fep.web.submission.dashboard.dto.DashboardResponse;
import com.puchain.fep.web.submission.dashboard.service.SubDashboardService;
import com.puchain.fep.web.sysmgmt.log.annotation.OperationLog;
import com.puchain.fep.web.sysmgmt.log.domain.OperationType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
