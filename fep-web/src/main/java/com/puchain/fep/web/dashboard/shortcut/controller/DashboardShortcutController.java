package com.puchain.fep.web.dashboard.shortcut.controller;

import com.puchain.fep.common.domain.ApiResult;
import com.puchain.fep.web.common.SecurityContextHelper;
import com.puchain.fep.web.dashboard.shortcut.dto.ShortcutCreateRequest;
import com.puchain.fep.web.dashboard.shortcut.dto.ShortcutReorderRequest;
import com.puchain.fep.web.dashboard.shortcut.dto.ShortcutResponse;
import com.puchain.fep.web.dashboard.shortcut.service.DashboardShortcutService;
import com.puchain.fep.web.sysmgmt.log.annotation.OperationLog;
import com.puchain.fep.web.sysmgmt.log.domain.OperationType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 首页快捷入口管理 REST Controller。
 *
 * <p>提供快捷入口 CRUD、排序及可见性切换接口。
 * 参见 PRD v1.3 §5.2.4 快捷入口（FR-WEB-DASH-QUICK）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/dashboard/shortcuts")
@Tag(name = "首页快捷入口", description = "PRD §5.2.4 首页快捷入口管理")
public class DashboardShortcutController {

    private final DashboardShortcutService shortcutService;

    /**
     * 构造 DashboardShortcutController。
     *
     * @param shortcutService 快捷入口管理服务
     */
    public DashboardShortcutController(final DashboardShortcutService shortcutService) {
        this.shortcutService = shortcutService;
    }

    /**
     * 查询当前用户的快捷入口列表。
     *
     * @param includeHidden 是否包含隐藏的快捷入口（默认 false）
     * @return 快捷入口列表
     */
    @GetMapping
    @OperationLog(module = "首页快捷入口", type = OperationType.QUERY, description = "查询快捷入口")
    @Operation(summary = "查询快捷入口", description = "查询当前用户的快捷入口列表，默认仅返回可见项")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public ApiResult<List<ShortcutResponse>> list(
            @Parameter(description = "是否包含隐藏项")
            @RequestParam(defaultValue = "false") final boolean includeHidden) {
        String userId = SecurityContextHelper.currentUserId();
        List<ShortcutResponse> result = includeHidden
                ? shortcutService.listAll(userId)
                : shortcutService.listVisible(userId);
        return ApiResult.success(result);
    }

    /**
     * 创建快捷入口。
     *
     * @param request 创建请求
     * @return 新建快捷入口信息
     */
    @PostMapping
    @OperationLog(module = "首页快捷入口", type = OperationType.CREATE, description = "创建快捷入口")
    @Operation(summary = "创建快捷入口", description = "创建新的快捷入口，默认 visible=true")
    @ApiResponse(responseCode = "200", description = "创建成功")
    @ApiResponse(responseCode = "400", description = "参数校验失败")
    @ApiResponse(responseCode = "409", description = "名称重复")
    public ApiResult<ShortcutResponse> create(
            @Valid @RequestBody final ShortcutCreateRequest request) {
        String userId = SecurityContextHelper.currentUserId();
        return ApiResult.success(shortcutService.create(request, userId));
    }

    /**
     * 批量更新快捷入口排序。
     *
     * @param request 重排序请求
     * @return 空响应
     */
    @PutMapping("/reorder")
    @OperationLog(module = "首页快捷入口", type = OperationType.UPDATE, description = "重排序快捷入口")
    @Operation(summary = "重排序快捷入口", description = "批量更新快捷入口的排序序号")
    @ApiResponse(responseCode = "200", description = "更新成功")
    public ApiResult<Void> reorder(
            @Valid @RequestBody final ShortcutReorderRequest request) {
        shortcutService.reorder(request);
        return ApiResult.success();
    }

    /**
     * 切换快捷入口的可见性。
     *
     * @param shortcutId 快捷入口 ID
     * @return 更新后的快捷入口信息
     */
    @PutMapping("/{shortcutId}/toggle-visibility")
    @OperationLog(module = "首页快捷入口", type = OperationType.UPDATE, description = "切换快捷入口可见性")
    @Operation(summary = "切换可见性", description = "将快捷入口的 visible 字段取反")
    @ApiResponse(responseCode = "200", description = "操作成功")
    @ApiResponse(responseCode = "404", description = "快捷入口不存在")
    public ApiResult<ShortcutResponse> toggleVisibility(
            @Parameter(description = "快捷入口 ID") @PathVariable final String shortcutId) {
        return ApiResult.success(shortcutService.toggleVisibility(shortcutId));
    }

    /**
     * 删除快捷入口。
     *
     * @param shortcutId 快捷入口 ID
     * @return 空响应
     */
    @DeleteMapping("/{shortcutId}")
    @OperationLog(module = "首页快捷入口", type = OperationType.DELETE, description = "删除快捷入口")
    @Operation(summary = "删除快捷入口", description = "物理删除快捷入口")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @ApiResponse(responseCode = "404", description = "快捷入口不存在")
    public ApiResult<Void> delete(
            @Parameter(description = "快捷入口 ID") @PathVariable final String shortcutId) {
        shortcutService.delete(shortcutId);
        return ApiResult.success();
    }
}
