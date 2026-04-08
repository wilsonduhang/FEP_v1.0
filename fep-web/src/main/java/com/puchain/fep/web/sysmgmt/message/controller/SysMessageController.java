package com.puchain.fep.web.sysmgmt.message.controller;

import com.puchain.fep.common.domain.ApiResult;
import com.puchain.fep.common.domain.PageResult;
import com.puchain.fep.web.common.SecurityContextHelper;
import com.puchain.fep.web.sysmgmt.log.annotation.OperationLog;
import com.puchain.fep.web.sysmgmt.log.domain.OperationType;
import com.puchain.fep.web.sysmgmt.message.dto.MessageCreateRequest;
import com.puchain.fep.web.sysmgmt.message.dto.MessageResponse;
import com.puchain.fep.web.sysmgmt.message.service.SysMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 消息管理 REST API。
 *
 * <p>提供系统公告发布、消息列表查询、已读标记、未读计数、消息删除接口。
 * 支持广播（ALL）、指定用户（USER）、指定角色（ROLE）三种投递方式。
 * 参见 PRD v1.3 §5.10.4 消息管理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/sys/messages")
@Tag(name = "06. 消息管理", description = "系统公告 / 消息推送 / 已读管理")
public class SysMessageController {

    private final SysMessageService messageService;

    /**
     * 构造 SysMessageController。
     *
     * @param messageService 消息管理服务
     */
    public SysMessageController(final SysMessageService messageService) {
        this.messageService = messageService;
    }

    /**
     * 发布消息。
     *
     * <p>从 SecurityContext 中获取当前用户 ID 作为发送者。
     * receiverType=ALL 时 receiverId 可为 null；USER/ROLE 类型必须提供 receiverId。</p>
     *
     * @param request 消息创建请求
     * @return 已发布的消息信息
     */
    @PostMapping
    @Operation(summary = "发布消息", description = "发布系统公告或推送消息，支持广播/指定用户/指定角色")
    @ApiResponse(responseCode = "200", description = "发布成功")
    @ApiResponse(responseCode = "400", description = "参数校验失败或 receiverId 缺失")
    @OperationLog(module = "消息管理", type = OperationType.CREATE)
    public ApiResult<MessageResponse> publish(@Valid @RequestBody final MessageCreateRequest request) {
        return ApiResult.success(messageService.publish(request, SecurityContextHelper.currentUserId()));
    }

    /**
     * 管理员消息列表（分页）。
     *
     * <p>返回全部状态为 NORMAL 的消息，按创建时间降序排列。</p>
     *
     * @param pageNum  页码（1-based，默认 1）
     * @param pageSize 每页大小（默认 20）
     * @return 分页消息列表
     */
    @GetMapping
    @Operation(summary = "管理员消息列表", description = "分页查询所有 NORMAL 状态消息，按创建时间降序")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @OperationLog(module = "消息管理", type = OperationType.QUERY)
    public ApiResult<PageResult<MessageResponse>> adminList(
            @Parameter(description = "页码（1-based）")
            @RequestParam(defaultValue = "1") final int pageNum,
            @Parameter(description = "每页大小")
            @RequestParam(defaultValue = "20") final int pageSize) {
        return ApiResult.success(messageService.adminList(pageNum, pageSize));
    }

    /**
     * 查询当前用户的消息列表（分页）。
     *
     * <p>可见性规则：ALL 广播 + 指定到本用户 + 指定到本用户所属角色。
     * 从 SecurityContext 中获取当前用户 ID。</p>
     *
     * @param pageNum  页码（1-based，默认 1）
     * @param pageSize 每页大小（默认 20）
     * @return 分页消息列表（含已读状态）
     */
    @GetMapping("/mine")
    @Operation(summary = "我的消息列表", description = "查询当前用户可见的消息（广播+个人+角色），含已读状态")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public ApiResult<PageResult<MessageResponse>> myMessages(
            @Parameter(description = "页码（1-based）")
            @RequestParam(defaultValue = "1") final int pageNum,
            @Parameter(description = "每页大小")
            @RequestParam(defaultValue = "20") final int pageSize) {
        return ApiResult.success(messageService.myMessages(SecurityContextHelper.currentUserId(), pageNum, pageSize));
    }

    /**
     * 查询当前用户未读消息数量。
     *
     * @return 未读消息数量
     */
    @GetMapping("/mine/unread-count")
    @Operation(summary = "未读消息数量", description = "查询当前用户未读消息总数")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public ApiResult<Long> unreadCount() {
        return ApiResult.success(messageService.unreadCount(SecurityContextHelper.currentUserId()));
    }

    /**
     * 标记单条消息为已读。
     *
     * <p>若已读则幂等忽略；消息不存在时抛出业务异常。</p>
     *
     * @param messageId 消息 ID
     * @return 空响应
     */
    @PostMapping("/{messageId}/read")
    @Operation(summary = "标记消息已读", description = "将指定消息标记为已读，已读时幂等忽略")
    @ApiResponse(responseCode = "200", description = "标记成功")
    @ApiResponse(responseCode = "400", description = "消息不存在")
    public ApiResult<Void> markRead(
            @Parameter(description = "消息 ID") @PathVariable final String messageId) {
        messageService.markRead(messageId, SecurityContextHelper.currentUserId());
        return ApiResult.success();
    }

    /**
     * 标记当前用户所有可见消息为已读。
     *
     * @return 空响应
     */
    @PostMapping("/read-all")
    @Operation(summary = "全部已读", description = "将当前用户所有可见消息标记为已读")
    @ApiResponse(responseCode = "200", description = "操作成功")
    public ApiResult<Void> markAllRead() {
        messageService.markAllRead(SecurityContextHelper.currentUserId());
        return ApiResult.success();
    }

    /**
     * 逻辑删除消息。
     *
     * <p>设置 messageStatus=DELETED，不物理删除数据库记录。</p>
     *
     * @param messageId 消息 ID
     * @return 空响应
     */
    @DeleteMapping("/{messageId}")
    @Operation(summary = "删除消息", description = "逻辑删除消息（设置 messageStatus=DELETED），不可恢复")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @ApiResponse(responseCode = "400", description = "消息不存在")
    @OperationLog(module = "消息管理", type = OperationType.DELETE, description = "逻辑删除消息")
    public ApiResult<Void> delete(
            @Parameter(description = "消息 ID") @PathVariable final String messageId) {
        messageService.delete(messageId);
        return ApiResult.success();
    }

}
