package com.puchain.fep.web.submission.record.controller;

import com.puchain.fep.common.domain.ApiResult;
import com.puchain.fep.web.submission.record.dto.MessageSummaryResponse;
import com.puchain.fep.web.submission.record.service.SubSubmissionRecordService;
import com.puchain.fep.web.sysmgmt.log.annotation.OperationLog;
import com.puchain.fep.web.sysmgmt.log.domain.OperationType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 报文数据列表 REST Controller（§5.5.5）。
 *
 * <p>提供按报文类型聚合的报送汇总统计。
 * 参见 PRD v1.3 §5.5.5 报文数据列表（FR-WEB-SUB-LIST）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/submission/message-summary")
@Tag(name = "报文数据列表", description = "PRD §5.5.5 按报文类型聚合展示报送汇总")
public class SubMessageSummaryController {

    private final SubSubmissionRecordService recordService;

    /**
     * 构造 SubMessageSummaryController。
     *
     * @param recordService 报送记录管理服务
     */
    public SubMessageSummaryController(
            final SubSubmissionRecordService recordService) {
        this.recordService = recordService;
    }

    /**
     * 获取报文汇总统计列表。
     *
     * @return 报文汇总列表
     */
    @GetMapping
    @OperationLog(module = "报文数据列表", type = OperationType.QUERY,
            description = "查询报文汇总统计")
    @Operation(summary = "报文汇总统计",
            description = "按报文类型聚合展示总数/已推送/待推送")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public ApiResult<List<MessageSummaryResponse>> getMessageSummary() {
        return ApiResult.success(recordService.getMessageSummary());
    }
}
