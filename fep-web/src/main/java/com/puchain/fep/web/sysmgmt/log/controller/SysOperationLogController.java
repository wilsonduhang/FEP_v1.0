package com.puchain.fep.web.sysmgmt.log.controller;

import com.puchain.fep.common.domain.ApiResult;
import com.puchain.fep.common.domain.PageResult;
import com.puchain.fep.web.sysmgmt.log.annotation.OperationLog;
import com.puchain.fep.web.sysmgmt.log.audit.AuditChainVerifier;
import com.puchain.fep.web.sysmgmt.log.audit.ChainVerifyResult;
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
import java.util.Locale;

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
    private final AuditChainVerifier auditChainVerifier;

    /**
     * 构造 SysOperationLogController。
     *
     * @param logService         操作日志服务
     * @param auditChainVerifier 审计链篡改检测（GM S5）
     */
    public SysOperationLogController(final SysOperationLogService logService,
            final AuditChainVerifier auditChainVerifier) {
        this.logService = logService;
        this.auditChainVerifier = auditChainVerifier;
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
     * @param traceId     链路追踪 ID（可选，精确匹配，GM S5 架构 §1219）
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
            @Parameter(description = "链路追踪 ID（精确匹配，GM S5）")
            @RequestParam(required = false) final String traceId,
            @Parameter(description = "页码（1-based）")
            @RequestParam(defaultValue = "1") final int pageNum,
            @Parameter(description = "每页大小")
            @RequestParam(defaultValue = "20") final int pageSize) {
        return ApiResult.success(logService.search(
                userAccount, module, startTime, endTime, traceId, pageNum, pageSize));
    }

    /**
     * 审计链完整性校验（GM S5，架构 §1219 篡改可检；EFF-S5-1 增量化）。
     *
     * <p>incremental（默认）= checkpoint 锚后增量段（O(Δ)，跳过已验段且不检测
     * 已验段内事后篡改；缺锚退化全链）；full = GENESIS 起全链权威校验
     * （O(n)×约1-3ms/行，SM2 验签主导——链长 ≤1 万行可同步调用，超出建议
     * 低频运维窗口执行并周期性以 full 复核）。本端点自身经
     * {@code @OperationLog} 入链。</p>
     *
     * @param mode 校验模式 full / incremental（大小写不敏感，默认 incremental）
     * @return 校验结果（intact / firstBreakSeq / breakType / mode / checkpointSeq）
     */
    @GetMapping("/integrity")
    @Operation(summary = "审计链完整性校验",
            description = "重算 SM3 hash 链 + 逐行 SM2 验签，返回链完整性与首断点（架构 §1219）。"
                    + "mode=incremental（默认）自 checkpoint 锚增量校验 O(Δ)，跳过已验段、"
                    + "不检测已验段内事后篡改，缺锚自动退化全链；mode=full 全链权威校验"
                    + " O(n)×约1-3ms/行（SM2 验签主导）——链长超 1 万行建议低频运维窗口调用，"
                    + "并周期性以 full 复核增量结果")
    @ApiResponse(responseCode = "200", description = "校验完成（intact 字段标识链是否完整）")
    @ApiResponse(responseCode = "400", description = "mode 取值非法（仅 full / incremental）")
    @OperationLog(module = "日志管理", type = OperationType.QUERY, description = "审计链完整性校验")
    public ApiResult<ChainVerifyResult> integrity(
            @Parameter(description = "校验模式：incremental（默认）/ full")
            @RequestParam(defaultValue = "incremental") final String mode) {
        return ApiResult.success(auditChainVerifier.verifyChain(
                AuditChainVerifier.VerifyMode.valueOf(mode.toUpperCase(Locale.ROOT))));
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
