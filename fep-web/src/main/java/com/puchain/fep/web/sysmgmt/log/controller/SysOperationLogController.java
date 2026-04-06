package com.puchain.fep.web.sysmgmt.log.controller;

import com.puchain.fep.common.domain.ApiResult;
import com.puchain.fep.common.domain.PageResult;
import com.puchain.fep.web.sysmgmt.log.annotation.OperationLog;
import com.puchain.fep.web.sysmgmt.log.domain.OperationType;
import com.puchain.fep.web.sysmgmt.log.dto.OperationLogResponse;
import com.puchain.fep.web.sysmgmt.log.service.SysOperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 操作日志查询 REST API。
 *
 * <p>提供按时间范围、用户账号、功能模块筛选的分页查询，以及按 ID 查询单条记录。
 * 参见 PRD v1.3 §5.10.6 日志管理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/sys/logs")
@Tag(name = "05. 操作日志", description = "操作日志查询 / 筛选")
public class SysOperationLogController {

    private final SysOperationLogService logService;

    /**
     * 构造 SysOperationLogController。
     *
     * @param logService 操作日志服务
     */
    public SysOperationLogController(final SysOperationLogService logService) {
        this.logService = logService;
    }

    /**
     * 分页查询操作日志。
     *
     * <p>支持按操作人账号（模糊）、功能模块（精确）、时间范围（含边界）筛选。</p>
     *
     * @param userAccount 操作人账号（可选，模糊匹配）
     * @param module      功能模块（可选，精确匹配）
     * @param startTime   操作时间起始（可选，ISO 日期时间格式）
     * @param endTime     操作时间截止（可选，ISO 日期时间格式）
     * @param pageNum     页码（1-based，默认 1）
     * @param pageSize    每页大小（默认 20）
     * @return 分页操作日志列表
     */
    @GetMapping
    @Operation(summary = "查询操作日志", description = "按用户账号、功能模块、时间范围分页筛选操作日志")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @OperationLog(module = "日志管理", type = OperationType.QUERY, description = "查询操作日志")
    public ApiResult<PageResult<OperationLogResponse>> search(
            @Parameter(description = "操作人账号（模糊匹配）")
            @RequestParam(required = false) final String userAccount,
            @Parameter(description = "功能模块（精确匹配）")
            @RequestParam(required = false) final String module,
            @Parameter(description = "操作时间起始（ISO 格式，如 2026-01-01T00:00:00）")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime startTime,
            @Parameter(description = "操作时间截止（ISO 格式，如 2026-12-31T23:59:59）")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime endTime,
            @Parameter(description = "页码（1-based）")
            @RequestParam(defaultValue = "1") final int pageNum,
            @Parameter(description = "每页大小")
            @RequestParam(defaultValue = "20") final int pageSize) {
        return ApiResult.success(logService.search(userAccount, module, startTime, endTime, pageNum, pageSize));
    }

    /**
     * 查询操作日志详情。
     *
     * @param logId 日志 ID
     * @return 操作日志详情
     */
    @GetMapping("/{logId}")
    @Operation(summary = "查询操作日志详情", description = "根据日志 ID 查询单条操作日志记录")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @ApiResponse(responseCode = "404", description = "日志记录不存在")
    public ApiResult<OperationLogResponse> findById(
            @Parameter(description = "日志 ID") @PathVariable final String logId) {
        return ApiResult.success(logService.findById(logId));
    }
}
