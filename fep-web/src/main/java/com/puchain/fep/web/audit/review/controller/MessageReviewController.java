package com.puchain.fep.web.audit.review.controller;

import com.puchain.fep.common.domain.ApiResult;
import com.puchain.fep.common.domain.PageResult;
import com.puchain.fep.web.audit.review.domain.ReviewStatus;
import com.puchain.fep.web.audit.review.dto.ReviewDecisionRequest;
import com.puchain.fep.web.audit.review.dto.ReviewTaskResponse;
import com.puchain.fep.web.audit.review.service.MessageReviewTaskService;
import com.puchain.fep.web.sysmgmt.log.annotation.OperationLog;
import com.puchain.fep.web.sysmgmt.log.domain.OperationType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 报文人工审核 REST API（PRD v1.3 §5.8 多级审核 Phase2，FR-WEB-AUDIT-REVIEW）。
 *
 * <p>提供业务规则失败（PROC_8507）报文的审核任务列表/详情/通过/驳回。
 * 访问控制由 {@code SecurityConfiguration} 的 URL 规则
 * {@code /api/v1/audit/reviews/** → hasRole('SYSTEM_ADMIN')} 强制（本项目未启用方法级
 * {@code @EnableMethodSecurity}，故 {@code @PreAuthorize} 仅作文档/防御标注，与既有
 * {@code CallbackDlqController} 一致）。业务人员角色 + 细粒度权限点随 Phase3 UI + RBAC 对齐 ticket。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/audit/reviews")
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
@Tag(name = "数据校验审核", description = "PRD §5.8 业务规则失败报文人工审核（Phase2 单级）")
public class MessageReviewController {

    private final MessageReviewTaskService service;

    /**
     * 构造审核控制器。
     *
     * @param service 审核任务服务
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Spring-managed singleton stored by reference per container contract")
    public MessageReviewController(final MessageReviewTaskService service) {
        this.service = service;
    }

    /**
     * 分页查询审核任务（最新优先）。
     *
     * @param status   审核状态过滤（可空，空为全部）
     * @param pageNum  页码（1-based，默认 1）
     * @param pageSize 每页条数（默认 20，服务端上限 200）
     * @return 分页审核任务列表
     */
    @GetMapping
    @OperationLog(module = "数据校验审核", type = OperationType.QUERY, description = "审核任务列表")
    @Operation(summary = "审核任务列表", description = "按状态筛选 + 分页查询业务规则失败报文的审核任务")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public ApiResult<PageResult<ReviewTaskResponse>> list(
            @Parameter(description = "审核状态（PENDING/APPROVED/REJECTED，空为全部）")
            @RequestParam(required = false) final ReviewStatus status,
            @Parameter(description = "页码（从1开始）")
            @RequestParam(defaultValue = "1") final int pageNum,
            @Parameter(description = "每页条数")
            @RequestParam(defaultValue = "20") final int pageSize) {
        return ApiResult.success(service.list(status, pageNum, pageSize));
    }

    /**
     * 查询审核任务详情。
     *
     * @param reviewId 审核任务 id
     * @return 审核任务详情
     */
    @GetMapping("/{reviewId}")
    @OperationLog(module = "数据校验审核", type = OperationType.QUERY, description = "审核任务详情")
    @Operation(summary = "审核任务详情", description = "按 id 查询单条审核任务")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @ApiResponse(responseCode = "404", description = "审核任务不存在")
    public ApiResult<ReviewTaskResponse> detail(
            @Parameter(description = "审核任务 id") @PathVariable final String reviewId) {
        return ApiResult.success(service.getById(reviewId));
    }

    /**
     * 审核通过。
     *
     * @param reviewId 审核任务 id
     * @param request  审核意见（可空）
     * @param auth     当前认证用户（取 username 作 reviewerId）
     * @return 成功响应
     */
    @PutMapping("/{reviewId}/approve")
    @OperationLog(module = "数据校验审核", type = OperationType.UPDATE, description = "审核通过")
    @Operation(summary = "审核通过", description = "单级一次通过即终结；多级逐级推进")
    @ApiResponse(responseCode = "200", description = "操作成功")
    @ApiResponse(responseCode = "404", description = "审核任务不存在")
    public ApiResult<Void> approve(
            @Parameter(description = "审核任务 id") @PathVariable final String reviewId,
            @RequestBody(required = false) final ReviewDecisionRequest request,
            final Authentication auth) {
        service.approve(reviewId, auth.getName(), request == null ? null : request.comment());
        return ApiResult.success();
    }

    /**
     * 审核驳回（原因必填）。
     *
     * @param reviewId 审核任务 id
     * @param request  驳回原因（{@code comment} 字段，必填非空白）
     * @param auth     当前认证用户（取 username 作 reviewerId）
     * @return 成功响应
     */
    @PutMapping("/{reviewId}/reject")
    @OperationLog(module = "数据校验审核", type = OperationType.UPDATE, description = "审核驳回")
    @Operation(summary = "审核驳回", description = "任一层驳回即终结，原因必填")
    @ApiResponse(responseCode = "200", description = "操作成功")
    @ApiResponse(responseCode = "400", description = "驳回原因为空")
    @ApiResponse(responseCode = "404", description = "审核任务不存在")
    public ApiResult<Void> reject(
            @Parameter(description = "审核任务 id") @PathVariable final String reviewId,
            @RequestBody(required = false) final ReviewDecisionRequest request,
            final Authentication auth) {
        service.reject(reviewId, auth.getName(), request == null ? null : request.comment());
        return ApiResult.success();
    }
}
