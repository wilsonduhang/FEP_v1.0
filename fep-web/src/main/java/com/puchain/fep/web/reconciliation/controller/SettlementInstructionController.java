package com.puchain.fep.web.reconciliation.controller;

import com.puchain.fep.common.domain.ApiResult;
import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.processor.body.supplychain.PlatPay3115;
import com.puchain.fep.processor.body.supplychain.QsInfo;
import com.puchain.fep.processor.reconciliation.ClearingInstructionRecord;
import com.puchain.fep.processor.reconciliation.ClearingInstructionService;
import com.puchain.fep.web.reconciliation.dto.QsInfoRequest;
import com.puchain.fep.web.reconciliation.dto.SettlementInstructionDetailResponse;
import com.puchain.fep.web.reconciliation.dto.SettlementInstructionRequest;
import com.puchain.fep.web.reconciliation.service.SettlementInstructionQueryService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 资金清算指令 REST Controller（PRD §2138 + §1995 + §5.3.2.12）。
 *
 * <p>P2e Task 7 — 暴露 2 个端点：</p>
 * <ul>
 *   <li>{@code POST /api/v1/settlement/instruction} — 发起 3115 清算指令，
 *       委派 {@link ClearingInstructionService#initiateOutbound(PlatPay3115, String)}</li>
 *   <li>{@code GET /api/v1/settlement/instruction/{id}} — 查询指定 platPayNo
 *       下所有 qsSerialNo 行；未命中 → {@link FepErrorCode#CLEAR_INSTRUCTION_NOT_FOUND}</li>
 * </ul>
 *
 * <h3>PK7 字段守护（Mode E 安全集成边界）</h3>
 * <p>{@link SettlementInstructionRequest} 故意不暴露 {@code SignElement} /
 * {@code qsfqSign} / {@code PlatSign} 三个 PK7 签名字段；Controller 在 build
 * {@link PlatPay3115} 时把它们留 null。{@link ClearingInstructionService#initiateOutbound}
 * 仍会在 service 层做 null 守护，提供深度防御。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/settlement/instruction")
@Tag(name = "清算指令管理", description = "PRD §2138 + §5.3.2.12 资金清算指令发起/查询")
public class SettlementInstructionController {

    private final ClearingInstructionService clearingService;
    private final SettlementInstructionQueryService queryService;

    /**
     * Constructs the controller.
     *
     * @param clearingService domain service for outbound 3115 instruction handling, non-null
     * @param queryService    read-side query service, non-null
     */
    public SettlementInstructionController(final ClearingInstructionService clearingService,
                                           final SettlementInstructionQueryService queryService) {
        this.clearingService = Objects.requireNonNull(clearingService, "clearingService");
        this.queryService = Objects.requireNonNull(queryService, "queryService");
    }

    /**
     * 发起 3115 资金清算指令（PRD §2138 + §5.3.2.12）。
     *
     * <p>委派 {@link ClearingInstructionService#initiateOutbound(PlatPay3115, String)}
     * 完成路由校验 + 业务规则校验 + 复合主键唯一性，并按 {@code qsInfo} 列表
     * 逐条落 {@code PENDING} 行。</p>
     *
     * @param request 清算指令发起请求 DTO（带 platPayNo + qsInfo[]）
     * @return 落库后的 PENDING 指令记录列表
     */
    @PostMapping
    @OperationLog(module = "清算指令管理", type = OperationType.CREATE,
            description = "发起 3115 清算指令")
    @Operation(summary = "发起清算指令",
            description = "按 qsInfo 列表逐条落 PENDING 行，调用 initiateOutbound")
    @ApiResponse(responseCode = "200", description = "发起成功")
    @ApiResponse(responseCode = "400", description = "参数校验失败 / 业务规则违例")
    public ApiResult<List<SettlementInstructionDetailResponse>> initiate(
            @Valid @RequestBody final SettlementInstructionRequest request) {
        final PlatPay3115 body = toPlatPay3115(request);
        final String messageId = UUID.randomUUID().toString().replace("-", "");
        final List<ClearingInstructionRecord> saved = clearingService.initiateOutbound(body, messageId);
        final List<SettlementInstructionDetailResponse> dtos = saved.stream()
                .map(SettlementInstructionDetailResponse::from)
                .toList();
        return ApiResult.success(dtos);
    }

    /**
     * 查询指定 platPayNo 下所有清算指令明细（PRD §2138）。
     *
     * @param id 清算指令平台编号（即 instructionId / platPayNo）
     * @return 该指令下所有 qsSerialNo 行列表（按 qsSerialNo 升序）
     */
    @GetMapping("/{id}")
    @OperationLog(module = "清算指令管理", type = OperationType.QUERY,
            description = "查询清算指令")
    @Operation(summary = "查询清算指令明细",
            description = "按 platPayNo 返回该指令下全部 qsSerialNo 行")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @ApiResponse(responseCode = "400", description = "记录不存在")
    public ApiResult<List<SettlementInstructionDetailResponse>> getByPlatPayNo(
            @Parameter(description = "清算指令平台编号", required = true)
            @PathVariable final String id) {
        final List<ClearingInstructionRecord> rows = queryService.findByPlatPayNo(id);
        if (rows.isEmpty()) {
            throw new FepBusinessException(
                    FepErrorCode.CLEAR_INSTRUCTION_NOT_FOUND,
                    "clearing instruction not found: platPayNo=" + id);
        }
        final List<SettlementInstructionDetailResponse> dtos = rows.stream()
                .map(SettlementInstructionDetailResponse::from)
                .toList();
        return ApiResult.success(dtos);
    }

    /**
     * Builds a {@link PlatPay3115} business body from the request DTO. PK7
     * signature fields are intentionally left {@code null}: the Mode E security
     * integration is pending, and the service layer enforces that contract.
     *
     * @param req validated request DTO
     * @return populated PlatPay3115 body
     */
    private static PlatPay3115 toPlatPay3115(final SettlementInstructionRequest req) {
        final PlatPay3115 body = new PlatPay3115();
        body.setSerialNo(req.getSerialNo());
        body.setSendNodeCode(req.getSendNodeCode());
        body.setDesNodeCode(req.getDesNodeCode());
        body.setPlatPayNo(req.getPlatPayNo());
        // PK7 fields (SignElement / qsfqSign / PlatSign) intentionally left null
        // — Mode E security integration TBD (see ClearingInstructionService guard).
        final List<QsInfo> qsList = new ArrayList<>(req.getQsInfo().size());
        for (QsInfoRequest qi : req.getQsInfo()) {
            final QsInfo qs = new QsInfo();
            qs.setQsSerialNo(qi.getQsSerialNo());
            qs.setFkfAccName(qi.getFkfAccName());
            qs.setFkfAccNo(qi.getFkfAccNo());
            qs.setSkfAccName(qi.getSkfAccName());
            qs.setSkfAccNo(qi.getSkfAccNo());
            qs.setAmt(qi.getAmt() == null ? null : qi.getAmt().toPlainString());
            qs.setWishDate(qi.getWishDate());
            qsList.add(qs);
        }
        body.setQsInfo(qsList);
        return body;
    }
}
