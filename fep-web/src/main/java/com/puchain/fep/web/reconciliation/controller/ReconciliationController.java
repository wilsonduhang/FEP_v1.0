package com.puchain.fep.web.reconciliation.controller;

import com.puchain.fep.common.domain.ApiResult;
import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.domain.PageResult;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.processor.reconciliation.ReconciliationRecord;
import com.puchain.fep.web.reconciliation.dto.DailyReconciliationRequest;
import com.puchain.fep.web.reconciliation.dto.ReconciliationDetailResponse;
import com.puchain.fep.web.reconciliation.dto.ReconciliationListResponse;
import com.puchain.fep.web.reconciliation.service.ReconciliationQueryService;
import com.puchain.fep.web.sysmgmt.log.annotation.OperationLog;
import com.puchain.fep.web.sysmgmt.log.domain.OperationType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 对账记录 REST Controller（PRD §2137 + §1983 + §5.3.2.13/14）。
 *
 * <p>P2e Task 7 — 暴露 3 个端点驱动 L5 业务对账引擎的查询：</p>
 * <ul>
 *   <li>{@code POST /api/v1/reconciliation/daily} — 触发当日对账查询（按 date + messageType
 *       聚合），未命中 → {@link FepErrorCode#RECON_NO_INBOUND}</li>
 *   <li>{@code GET /api/v1/reconciliation/{id}} — 单条详情，未命中 →
 *       {@link FepErrorCode#RECON_NOT_FOUND}</li>
 *   <li>{@code GET /api/v1/reconciliation} — 分页列表（可选 date / status / messageType）</li>
 * </ul>
 *
 * <h3>权限点（延后接入）</h3>
 * <p>P6d 既有 Controller 未启用 {@code @PreAuthorize}，本 Controller 保持一致。
 * RBAC {@code report:reconciliation:read} / {@code :write} 权限点接入延后到统一的
 * Spring Security {@code GlobalMethodSecurityConfiguration} 启用环节
 * （tracking ticket — P2e Task 9 / P6d 权限对齐）。</p>
 *
 * <h3>异常处理契约</h3>
 * <p>本 Controller 抛 {@link FepBusinessException}，由
 * {@link com.puchain.fep.common.exception.GlobalExceptionHandler#handleBusiness}
 * 自动转 ApiResult。{@code RECON_NOT_FOUND} 默认映射到 HTTP 400（非
 * {@link FepErrorCode#BIZ_5001} 故不会走 404 分支）— 参见 GlobalExceptionHandler 实现。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/reconciliation")
@Tag(name = "对账管理", description = "PRD §2137 + §5.3.2.13/14 对账记录查询/触发")
public class ReconciliationController {

    /** 业务日期解析格式（yyyyMMdd，对齐 BankCheckDay3116.checkDate）。 */
    private static final DateTimeFormatter BUSINESS_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** 默认分页大小。 */
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final ReconciliationQueryService queryService;

    /**
     * Constructs the controller.
     *
     * @param queryService read-side query service, non-null
     */
    public ReconciliationController(final ReconciliationQueryService queryService) {
        this.queryService = Objects.requireNonNull(queryService, "queryService");
    }

    /**
     * 触发当日对账查询（PRD §2137）。返回该业务日期下最近一条对账记录。
     *
     * @param request 当日对账请求 DTO，含 date(yyyyMMdd) + messageType(固定 3116)
     * @return 当日最近的对账记录详情
     */
    @PostMapping("/daily")
    @OperationLog(module = "对账管理", type = OperationType.QUERY,
            description = "触发当日对账查询")
    @Operation(summary = "触发当日对账",
            description = "查询指定日期的最近对账记录，未命中返回 RECON_NO_INBOUND")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @ApiResponse(responseCode = "400", description = "参数校验失败 / 数据缺失")
    public ApiResult<ReconciliationDetailResponse> triggerDaily(
            @Valid @RequestBody final DailyReconciliationRequest request) {
        final LocalDate businessDate = parseBusinessDate(request.getDate());
        final Optional<ReconciliationRecord> latest =
                queryService.findLatestByMessageTypeAndDate(request.getMessageType(), businessDate);
        if (latest.isEmpty()) {
            throw new FepBusinessException(
                    FepErrorCode.RECON_NO_INBOUND,
                    "no inbound reconciliation found for date=" + request.getDate()
                            + " messageType=" + request.getMessageType());
        }
        return ApiResult.success(ReconciliationDetailResponse.from(latest.get()));
    }

    /**
     * 查询单条对账记录详情（PRD §2137）。
     *
     * @param id 对账编号（{@code RC_YYYYMMDD_NNN}）
     * @return 对账记录详情
     */
    @GetMapping("/{id}")
    @OperationLog(module = "对账管理", type = OperationType.QUERY,
            description = "查询对账记录详情")
    @Operation(summary = "对账记录详情", description = "按 ID 查询对账记录")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @ApiResponse(responseCode = "400", description = "记录不存在")
    public ApiResult<ReconciliationDetailResponse> getById(
            @Parameter(description = "对账编号", required = true)
            @PathVariable final String id) {
        final Optional<ReconciliationRecord> opt = queryService.findById(id);
        if (opt.isEmpty()) {
            throw new FepBusinessException(
                    FepErrorCode.RECON_NOT_FOUND,
                    "reconciliation record not found: id=" + id);
        }
        return ApiResult.success(ReconciliationDetailResponse.from(opt.get()));
    }

    /**
     * 分页查询对账记录（PRD §2137）。可选过滤：date / status / messageType。
     *
     * @param date        对账业务日期（yyyy-MM-dd，可选）
     * @param status      对账状态（可选，PENDING / IN_PROGRESS / COMPLETED / DISCREPANCY）
     * @param messageType 报文类型（可选，3107 / 3108 / 3116）
     * @param pageNum     页码，1-based，默认 1
     * @param pageSize    每页大小，默认 20
     * @return 分页对账列表
     */
    @GetMapping
    @OperationLog(module = "对账管理", type = OperationType.QUERY,
            description = "分页查询对账记录")
    @Operation(summary = "分页查询对账",
            description = "按 date / status / messageType 过滤的分页查询")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public ApiResult<PageResult<ReconciliationListResponse>> search(
            @Parameter(description = "对账日期（yyyy-MM-dd）")
            @RequestParam(required = false) final String date,
            @Parameter(description = "对账状态")
            @RequestParam(required = false) final String status,
            @Parameter(description = "报文类型")
            @RequestParam(required = false) final String messageType,
            @Parameter(description = "页码（从 1 开始）")
            @RequestParam(defaultValue = "1") final int pageNum,
            @Parameter(description = "每页条数")
            @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) final int pageSize) {

        final LocalDate parsedDate = (date == null || date.isBlank()) ? null : parseIsoDate(date);
        final ReconciliationQueryService.PagedResult result =
                queryService.search(parsedDate, messageType, status, pageNum, pageSize);

        final List<ReconciliationListResponse> dtos = result.content().stream()
                .map(ReconciliationListResponse::from)
                .toList();
        return ApiResult.success(
                new PageResult<>(dtos, result.total(), pageNum, pageSize));
    }

    /**
     * Parses an 8-digit business date string (yyyyMMdd) into a {@link LocalDate}.
     *
     * @param raw business date string, non-null per {@code @NotBlank}
     * @return parsed local date
     * @throws FepBusinessException with {@link FepErrorCode#RECON_INVALID_DATE} when invalid
     */
    private LocalDate parseBusinessDate(final String raw) {
        try {
            return LocalDate.parse(raw, BUSINESS_DATE);
        } catch (DateTimeParseException e) {
            throw new FepBusinessException(
                    FepErrorCode.RECON_INVALID_DATE,
                    "invalid business date: " + raw, e);
        }
    }

    /**
     * Parses an ISO-8601 date string (yyyy-MM-dd) for query param compatibility.
     *
     * @param raw ISO date string
     * @return parsed local date
     * @throws FepBusinessException with {@link FepErrorCode#RECON_INVALID_DATE} when invalid
     */
    private LocalDate parseIsoDate(final String raw) {
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            throw new FepBusinessException(
                    FepErrorCode.RECON_INVALID_DATE,
                    "invalid ISO date: " + raw, e);
        }
    }
}
