package com.puchain.fep.web.callback.notification.controller;

import com.puchain.fep.common.domain.ApiResult;
import com.puchain.fep.web.callback.notification.dto.CallbackNotificationResponse;
import com.puchain.fep.web.callback.notification.service.CallbackNotificationService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 站内通知 REST API（当前登录用户视角）。
 *
 * <p>提供未读列表 / 未读数 / 标记已读。所有操作以认证用户（{@code Authentication#getName}）为边界，
 * 任意已认证用户仅能访问自己的通知。参见 PRD v1.3 §5.5.3 回调可靠性告警
 * （FR-INFRA-CALLBACK-ALERT）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "站内通知", description = "当前登录用户的未读通知列表 / 计数 / 标记已读")
public class CallbackNotificationController {

    private final CallbackNotificationService service;

    /**
     * 构造站内通知控制器。
     *
     * @param service 站内通知服务
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Spring-managed singleton stored by reference per container contract")
    public CallbackNotificationController(final CallbackNotificationService service) {
        this.service = service;
    }

    /**
     * 查询当前用户未读通知（最新优先）。
     *
     * @param auth 当前认证用户
     * @return 未读通知列表
     */
    @GetMapping("/unread")
    @Operation(summary = "未读通知列表", description = "返回当前登录用户的未读通知，最新优先")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public ApiResult<List<CallbackNotificationResponse>> unread(final Authentication auth) {
        return ApiResult.success(service.listUnread(auth.getName()));
    }

    /**
     * 统计当前用户未读通知数。
     *
     * @param auth 当前认证用户
     * @return 未读数
     */
    @GetMapping("/unread/count")
    @Operation(summary = "未读通知数", description = "返回当前登录用户的未读通知计数")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public ApiResult<Long> count(final Authentication auth) {
        return ApiResult.success(service.unreadCount(auth.getName()));
    }

    /**
     * 标记指定通知为已读（仅本人通知生效）。
     *
     * @param id   通知 id
     * @param auth 当前认证用户
     * @return 空成功响应
     */
    @PutMapping("/{id}/read")
    @Operation(summary = "标记已读", description = "标记当前用户的指定通知为已读，非本人通知静默忽略")
    @ApiResponse(responseCode = "200", description = "标记成功")
    public ApiResult<Void> markRead(
            @Parameter(description = "通知 id") @PathVariable final String id,
            final Authentication auth) {
        service.markRead(id, auth.getName());
        return ApiResult.success();
    }
}
