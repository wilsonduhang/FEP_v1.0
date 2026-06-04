package com.puchain.fep.web.callback.dlq.controller;

import com.puchain.fep.common.domain.ApiResult;
import com.puchain.fep.web.callback.dlq.dto.DlqEntryResponse;
import com.puchain.fep.web.callback.dlq.dto.DlqReplayResponse;
import com.puchain.fep.web.callback.dlq.service.CallbackReplayService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 回调死信队列管理 REST API（查看 + 复制重放）。
 *
 * <p>提供 DLQ 列表查询、复制重放、重放回溯链查询。复制重放保留原死信行作审计证据。
 * 仅管理员（ROLE_ADMIN）可访问。参见 PRD v1.3 §5.5.3 回调可靠性
 * （FR-INFRA-CALLBACK-DLQ-REPLAY）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/callback/dlq")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "回调死信队列管理", description = "死信查看 + 复制重放（原死信行保留作审计证据）")
public class CallbackDlqController {

    private final CallbackReplayService service;

    /**
     * 构造死信队列管理控制器。
     *
     * @param service 死信重放服务
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Spring-managed singleton stored by reference per container contract")
    public CallbackDlqController(final CallbackReplayService service) {
        this.service = service;
    }

    /**
     * 分页查询死信行（最新优先）。
     *
     * @param page 页码（0-based）
     * @param size 每页条数
     * @return 死信条目列表
     */
    @GetMapping
    @Operation(summary = "DLQ 列表", description = "分页查询 DEAD_LETTER 状态的死信行，最新优先")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public ApiResult<List<DlqEntryResponse>> list(
            @Parameter(description = "页码（0-based）") @RequestParam(defaultValue = "0") final int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") final int size) {
        return ApiResult.success(service.list(PageRequest.of(page, size)));
    }

    /**
     * 复制重放指定死信行（原死信行保留作审计证据）。
     *
     * @param dlqId 源死信行 id
     * @param auth  当前认证用户（取 username 作审计 replayedBy）
     * @return 重放响应（新行 id + 源死信 id + 重放时间）
     */
    @PostMapping("/{dlqId}/replay")
    @Operation(summary = "复制重放", description = "以原死信行为模板新建 PENDING 行，原 DEAD 行保留作审计证据")
    @ApiResponse(responseCode = "200", description = "重放成功")
    @ApiResponse(responseCode = "404", description = "死信行不存在或状态非 DEAD_LETTER")
    public ApiResult<DlqReplayResponse> replay(
            @Parameter(description = "源死信行 id") @PathVariable final String dlqId,
            final Authentication auth) {
        return ApiResult.success(service.replay(dlqId, auth.getName()));
    }

    /**
     * 查询从指定死信行衍生的重放链。
     *
     * @param dlqId 源死信行 id
     * @return 重放衍生行列表（无衍生时为空列表）
     */
    @GetMapping("/{dlqId}/chain")
    @Operation(summary = "重放回溯链", description = "查看从该 dlqId 衍生的所有重放行")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public ApiResult<List<DlqEntryResponse>> chain(
            @Parameter(description = "源死信行 id") @PathVariable final String dlqId) {
        return ApiResult.success(service.findReplayChain(dlqId));
    }
}
