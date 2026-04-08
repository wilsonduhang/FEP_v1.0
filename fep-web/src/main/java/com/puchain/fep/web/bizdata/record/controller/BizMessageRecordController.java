package com.puchain.fep.web.bizdata.record.controller;

import com.puchain.fep.common.domain.ApiResult;
import com.puchain.fep.common.domain.PageResult;
import com.puchain.fep.web.bizdata.domain.MessageDirection;
import com.puchain.fep.web.bizdata.record.domain.MessageProcessStatus;
import com.puchain.fep.web.bizdata.record.dto.RecordCreateRequest;
import com.puchain.fep.web.bizdata.record.dto.RecordResponse;
import com.puchain.fep.web.bizdata.record.dto.RecordSummaryItem;
import com.puchain.fep.web.bizdata.record.service.BizMessageRecordService;
import com.puchain.fep.web.sysmgmt.log.annotation.OperationLog;
import com.puchain.fep.web.sysmgmt.log.domain.OperationType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REST controller for business message record management.
 *
 * <p>See PRD v1.3 section 5.3.1 (FR-WEB-BIZ-LIST).</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/bizdata/records")
@Tag(name = "报文记录管理",
        description = "PRD section 5.3.1 报文记录查询 + 手动录入 + 重提 + 导出")
public class BizMessageRecordController {

    private final BizMessageRecordService recordService;

    /**
     * Construct BizMessageRecordController.
     *
     * @param recordService message record service
     */
    public BizMessageRecordController(
            final BizMessageRecordService recordService) {
        this.recordService = recordService;
    }

    /**
     * Search message records (paginated).
     *
     * @param messageCode  message code filter (optional)
     * @param status       process status filter (optional)
     * @param direction    direction filter (optional)
     * @param startDate    start date filter (optional)
     * @param endDate      end date filter (optional)
     * @param pageNum      page number (1-based, default 1)
     * @param pageSize     page size (default 20)
     * @return paginated record list
     */
    @GetMapping
    @OperationLog(module = "报文记录管理", type = OperationType.QUERY,
            description = "搜索报文记录")
    @Operation(summary = "搜索报文记录",
            description = "按报文编码/状态/方向/时间范围筛选，分页返回")
    @ApiResponse(responseCode = "200", description = "搜索成功")
    public ApiResult<PageResult<RecordResponse>> search(
            @Parameter(description = "报文编码")
            @RequestParam(required = false) final String messageCode,
            @Parameter(description = "处理状态")
            @RequestParam(required = false) final MessageProcessStatus status,
            @Parameter(description = "报文方向")
            @RequestParam(required = false) final MessageDirection direction,
            @Parameter(description = "开始时间")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            final LocalDateTime startDate,
            @Parameter(description = "结束时间")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            final LocalDateTime endDate,
            @Parameter(description = "页码（从1开始）")
            @RequestParam(defaultValue = "1") final int pageNum,
            @Parameter(description = "每页条数")
            @RequestParam(defaultValue = "20") final int pageSize) {
        return ApiResult.success(recordService.search(
                messageCode, status, direction,
                startDate, endDate, pageNum, pageSize));
    }

    /**
     * Get message summary aggregated by message code.
     *
     * @return summary list
     */
    @GetMapping("/summary")
    @OperationLog(module = "报文记录管理", type = OperationType.QUERY,
            description = "报文记录汇总统计")
    @Operation(summary = "报文记录汇总统计",
            description = "按报文类型汇总：总数/成功/待处理/失败")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public ApiResult<List<RecordSummaryItem>> getSummary() {
        return ApiResult.success(recordService.getSummary());
    }

    /**
     * Get a message record by ID (increments access count).
     *
     * @param recordId record ID
     * @return record detail with xmlContent
     */
    @GetMapping("/{recordId}")
    @OperationLog(module = "报文记录管理", type = OperationType.QUERY,
            description = "查询报文记录详情")
    @Operation(summary = "报文记录详情",
            description = "按 ID 查询报文记录，含 xmlContent，访问次数自增")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @ApiResponse(responseCode = "404", description = "报文记录不存在")
    public ApiResult<RecordResponse> getById(
            @Parameter(description = "记录 ID")
            @PathVariable final String recordId) {
        return ApiResult.success(recordService.getById(recordId));
    }

    /**
     * Create a new message record (manual entry).
     *
     * @param request creation request
     * @return created record
     */
    @PostMapping
    @OperationLog(module = "报文记录管理", type = OperationType.CREATE,
            description = "手动录入报文记录")
    @Operation(summary = "手动录入报文记录",
            description = "手动创建报文记录，serialNo 不能重复")
    @ApiResponse(responseCode = "200", description = "创建成功")
    @ApiResponse(responseCode = "400", description = "参数校验失败")
    @ApiResponse(responseCode = "409", description = "流水号已存在")
    public ApiResult<RecordResponse> create(
            @Valid @RequestBody final RecordCreateRequest request) {
        return ApiResult.success(recordService.create(request));
    }

    /**
     * Resubmit a failed message record.
     *
     * @param recordId record ID
     * @return updated record
     */
    @PostMapping("/{recordId}/resubmit")
    @OperationLog(module = "报文记录管理", type = OperationType.UPDATE,
            description = "重提报文记录")
    @Operation(summary = "重提报文记录",
            description = "仅 FAILED 状态可重提（重置为 PENDING）")
    @ApiResponse(responseCode = "200", description = "重提成功")
    @ApiResponse(responseCode = "404", description = "报文记录不存在")
    @ApiResponse(responseCode = "409", description = "当前状态不允许重提")
    public ApiResult<RecordResponse> resubmit(
            @Parameter(description = "记录 ID")
            @PathVariable final String recordId) {
        return ApiResult.success(recordService.resubmit(recordId));
    }

    /**
     * Export message records asynchronously.
     *
     * @param messageCode message code filter (optional)
     * @param status      process status filter (optional)
     * @param direction   direction filter (optional)
     * @param startDate   start date filter (optional)
     * @param endDate     end date filter (optional)
     * @return download task ID
     */
    @PostMapping("/export")
    @OperationLog(module = "报文记录管理", type = OperationType.QUERY,
            description = "导出报文记录")
    @Operation(summary = "异步导出报文记录",
            description = "异步导出 Excel，单次上限 10000 条，返回下载任务 ID")
    @ApiResponse(responseCode = "200", description = "导出任务已创建")
    public ApiResult<String> exportRecords(
            @Parameter(description = "报文编码")
            @RequestParam(required = false) final String messageCode,
            @Parameter(description = "处理状态")
            @RequestParam(required = false) final MessageProcessStatus status,
            @Parameter(description = "报文方向")
            @RequestParam(required = false) final MessageDirection direction,
            @Parameter(description = "开始时间")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            final LocalDateTime startDate,
            @Parameter(description = "结束时间")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            final LocalDateTime endDate) {
        return ApiResult.success(recordService.exportRecords(
                messageCode, status, direction, startDate, endDate));
    }
}
