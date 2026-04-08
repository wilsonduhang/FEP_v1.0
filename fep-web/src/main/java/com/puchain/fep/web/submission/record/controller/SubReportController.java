package com.puchain.fep.web.submission.record.controller;

import com.puchain.fep.common.domain.ApiResult;
import com.puchain.fep.common.domain.PageResult;
import com.puchain.fep.web.submission.record.dto.SubmissionRecordResponse;
import com.puchain.fep.web.submission.record.service.SubSubmissionRecordService;
import com.puchain.fep.web.sysmgmt.log.annotation.OperationLog;
import com.puchain.fep.web.sysmgmt.log.domain.OperationType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import java.util.Map;

/**
 * 报送管理 REST Controller（§5.6）。
 *
 * <p>提供报送记录搜索、详情、手动上传、推送触发、
 * 阻塞记录查询、按类型查看及趋势统计。
 * 参见 PRD v1.3 §5.6 报送管理
 * （FR-WEB-REP-UPLOAD / FR-WEB-REP-LIST /
 *  FR-WEB-REP-VIEW / FR-WEB-REP-PUSH）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/report")
@Tag(name = "报送管理", description = "PRD §5.6 报送记录管理/手动上传/推送/查看")
public class SubReportController {

    private final SubSubmissionRecordService recordService;

    /**
     * 构造 SubReportController。
     *
     * @param recordService 报送记录管理服务
     */
    public SubReportController(
            final SubSubmissionRecordService recordService) {
        this.recordService = recordService;
    }

    /**
     * 搜索报送记录（分页，§5.6.2）。
     *
     * @param keyword   关键字（可选，匹配报文名称或业务编号）
     * @param startTime 起始时间（可选）
     * @param endTime   截止时间（可选）
     * @param pageNum   页码（1-based，默认 1）
     * @param pageSize  每页大小（默认 10）
     * @return 分页报送记录列表
     */
    @GetMapping("/records")
    @OperationLog(module = "报送管理", type = OperationType.QUERY,
            description = "搜索报送记录")
    @Operation(summary = "搜索报送记录",
            description = "按关键字+时间范围搜索，分页返回")
    @ApiResponse(responseCode = "200", description = "搜索成功")
    public ApiResult<PageResult<SubmissionRecordResponse>> search(
            @Parameter(description = "关键字")
            @RequestParam(required = false) final String keyword,
            @Parameter(description = "起始时间")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            final LocalDateTime startTime,
            @Parameter(description = "截止时间")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            final LocalDateTime endTime,
            @Parameter(description = "页码（从1开始）")
            @RequestParam(defaultValue = "1") final int pageNum,
            @Parameter(description = "每页条数")
            @RequestParam(defaultValue = "10") final int pageSize) {
        return ApiResult.success(
                recordService.search(keyword, startTime, endTime,
                        pageNum, pageSize));
    }

    /**
     * 获取报送记录详情。
     *
     * @param recordId 记录 ID
     * @return 记录详情
     */
    @GetMapping("/records/{recordId}")
    @OperationLog(module = "报送管理", type = OperationType.QUERY,
            description = "查询报送记录详情")
    @Operation(summary = "报送记录详情", description = "按 ID 查询报送记录")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @ApiResponse(responseCode = "404", description = "记录不存在")
    public ApiResult<SubmissionRecordResponse> getById(
            @Parameter(description = "记录 ID")
            @PathVariable final String recordId) {
        return ApiResult.success(recordService.getById(recordId));
    }

    /**
     * 手动上传报送记录（§5.6.1）。
     *
     * @param messageType    报文类型
     * @param messageName    报文名称
     * @param businessTypeId 业务类型 ID
     * @param dataCount      数据条数
     * @param entryBy        录入人
     * @return 新建记录
     */
    @PostMapping("/upload")
    @OperationLog(module = "报送管理", type = OperationType.CREATE,
            description = "手动上传报送记录")
    @Operation(summary = "手动上传",
            description = "手动创建报送记录，状态默认为 PENDING")
    @ApiResponse(responseCode = "200", description = "上传成功")
    @ApiResponse(responseCode = "400", description = "参数校验失败")
    public ApiResult<SubmissionRecordResponse> upload(
            @Parameter(description = "报文类型", required = true)
            @RequestParam final String messageType,
            @Parameter(description = "报文名称", required = true)
            @RequestParam final String messageName,
            @Parameter(description = "业务类型 ID")
            @RequestParam(required = false) final String businessTypeId,
            @Parameter(description = "数据条数", required = true)
            @RequestParam final int dataCount,
            @Parameter(description = "录入人")
            @RequestParam(required = false) final String entryBy) {
        return ApiResult.success(
                recordService.createManualRecord(
                        messageType, messageName, businessTypeId,
                        dataCount, entryBy));
    }

    /**
     * 触发推送（§5.6.4）。
     *
     * @param recordIds 记录 ID 列表
     * @return 更新后的记录列表
     */
    @PostMapping("/push")
    @OperationLog(module = "报送管理", type = OperationType.UPDATE,
            description = "触发报文推送")
    @Operation(summary = "触发推送",
            description = "将指定记录状态从 PENDING 更新为 PUSHING")
    @ApiResponse(responseCode = "200", description = "推送触发成功")
    @ApiResponse(responseCode = "400", description = "无待推送记录")
    public ApiResult<List<SubmissionRecordResponse>> triggerPush(
            @RequestBody final List<String> recordIds) {
        return ApiResult.success(recordService.triggerPush(recordIds));
    }

    /**
     * 获取阻塞记录列表（PUSHING / FAILED，分页）。
     *
     * @param pageNum  页码（1-based，默认 1）
     * @param pageSize 每页大小（默认 100）
     * @return 分页阻塞记录列表
     */
    @GetMapping("/push/blocked")
    @OperationLog(module = "报送管理", type = OperationType.QUERY,
            description = "查询阻塞记录")
    @Operation(summary = "阻塞记录列表",
            description = "查询状态为 PUSHING 或 FAILED 的记录（分页）")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public ApiResult<PageResult<SubmissionRecordResponse>> getBlockedRecords(
            @Parameter(description = "页码（从1开始）")
            @RequestParam(defaultValue = "1") final int pageNum,
            @Parameter(description = "每页条数")
            @RequestParam(defaultValue = "100") final int pageSize) {
        return ApiResult.success(
                recordService.getBlockedRecords(pageNum, pageSize));
    }

    /**
     * 按报文类型查询记录（分页，§5.6.3）。
     *
     * @param messageType 报文类型
     * @param pageNum     页码（1-based，默认 1）
     * @param pageSize    每页大小（默认 10）
     * @return 分页记录列表
     */
    @GetMapping("/records/by-type/{messageType}")
    @OperationLog(module = "报送管理", type = OperationType.QUERY,
            description = "按类型查询报送数据")
    @Operation(summary = "按类型查看报送数据",
            description = "按报文类型分页查询报送记录")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public ApiResult<PageResult<SubmissionRecordResponse>> getByMessageType(
            @Parameter(description = "报文类型")
            @PathVariable final String messageType,
            @Parameter(description = "页码（从1开始）")
            @RequestParam(defaultValue = "1") final int pageNum,
            @Parameter(description = "每页条数")
            @RequestParam(defaultValue = "10") final int pageSize) {
        return ApiResult.success(
                recordService.getByMessageType(
                        messageType, pageNum, pageSize));
    }

    /**
     * 获取报文类型趋势数据（按月聚合）。
     *
     * @param messageType 报文类型
     * @return 趋势数据列表
     */
    @GetMapping("/records/by-type/{messageType}/trend")
    @OperationLog(module = "报送管理", type = OperationType.QUERY,
            description = "查询报文趋势数据")
    @Operation(summary = "报文趋势数据",
            description = "按月聚合指定报文类型的趋势")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public ApiResult<List<Map<String, Object>>> getTrend(
            @Parameter(description = "报文类型")
            @PathVariable final String messageType) {
        return ApiResult.success(recordService.getTrend(messageType));
    }
}
