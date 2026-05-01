package com.puchain.fep.web.sysmgmt.config.dirmap.controller;

import com.puchain.fep.common.domain.ApiResult;
import com.puchain.fep.common.domain.PageResult;
import com.puchain.fep.web.sysmgmt.config.dirmap.dto.DirMapConfigResponse;
import com.puchain.fep.web.sysmgmt.config.dirmap.dto.DirMapConfigUpdateRequest;
import com.puchain.fep.web.sysmgmt.config.dirmap.dto.DirMapHistoryResponse;
import com.puchain.fep.web.sysmgmt.config.dirmap.service.DirMapConfigAdminService;
import com.puchain.fep.web.sysmgmt.log.annotation.OperationLog;
import com.puchain.fep.web.sysmgmt.log.domain.OperationType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * DIR-MAP dynamic config admin REST API.
 *
 * <p>Surface area:
 * <ul>
 *   <li>{@code GET /api/v1/sys/config/dir-map} — paginated list (1-based pageNum
 *       per {@code feedback_pagination_adapter} red-line)</li>
 *   <li>{@code PUT /api/v1/sys/config/dir-map/{messageType}/{accessRole}} — edit
 *       single row, writes history audit, publishes
 *       {@link com.puchain.fep.processor.event.DirMapConfigChangedEvent}</li>
 *   <li>{@code GET /api/v1/sys/config/dir-map/{messageType}/{accessRole}/history}
 *       — fetch audit trail, latest first</li>
 * </ul>
 *
 * <p>Permission gating goes through {@code menuTree} filtering (V21 seed grants
 * super-admin only). No {@code @PreAuthorize} on methods and no
 * {@code meta.permission} on routes — see
 * {@code feedback_permission_code_vs_menu_code} red-line.
 *
 * @author FEP Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/sys/config/dir-map")
@Tag(name = "21. 系统配置 — 报文方向映射", description = "FR-MSG-DIR-MAP-CONFIG 动态可配")
public class DirMapConfigController {

    private final DirMapConfigAdminService service;

    /**
     * Construct controller wired to the admin service.
     *
     * @param service application service for DIR-MAP config CRUD + audit
     */
    public DirMapConfigController(DirMapConfigAdminService service) {
        this.service = service;
    }

    /**
     * Paginated list endpoint.
     *
     * <p>{@code pageNum} is 1-based per FEP front-end convention; we adapt to
     * 0-based {@code subList} indices here ({@code feedback_pagination_adapter}
     * red-line). Out-of-range pages return an empty list rather than throw.
     *
     * @param pageNum  1-based page number (default {@code 1})
     * @param pageSize page size (default {@code 100} so a single page covers
     *                 all 88 rows)
     * @return paginated {@link DirMapConfigResponse} slice
     */
    @GetMapping
    @OperationLog(module = "报文方向映射", type = OperationType.QUERY, description = "查询方向映射列表")
    @Operation(summary = "查询方向映射", description = "返回 88 条全量（pageSize 通常 88）")
    @ApiResponse(responseCode = "200")
    public ApiResult<PageResult<DirMapConfigResponse>> list(
            @Parameter(description = "页码 1-based") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "100") int pageSize) {
        // 1-based → 0-based slice (feedback_pagination_adapter 红线显式 adapter)
        List<DirMapConfigResponse> all = service.listAll();
        int from = Math.min(all.size(), Math.max(0, (pageNum - 1) * pageSize));
        int to = Math.min(all.size(), from + pageSize);
        return ApiResult.success(
                new PageResult<>(all.subList(from, to), all.size(), pageNum, pageSize));
    }

    /**
     * Edit a single DIR-MAP row.
     *
     * @param messageType message type code path variable
     * @param accessRole  access role name path variable
     * @param req         validated request body
     * @return updated row snapshot
     */
    @PutMapping("/{messageType}/{accessRole}")
    @OperationLog(module = "报文方向映射", type = OperationType.UPDATE, description = "编辑方向映射")
    @Operation(summary = "更新单条方向映射")
    @ApiResponse(responseCode = "200")
    public ApiResult<DirMapConfigResponse> update(
            @PathVariable String messageType,
            @PathVariable String accessRole,
            @Valid @RequestBody DirMapConfigUpdateRequest req) {
        return ApiResult.success(service.update(messageType, accessRole, req));
    }

    /**
     * Fetch audit history for a single DIR-MAP row, newest first.
     *
     * @param messageType message type code path variable
     * @param accessRole  access role name path variable
     * @return list of {@link DirMapHistoryResponse}, latest first
     */
    @GetMapping("/{messageType}/{accessRole}/history")
    @OperationLog(module = "报文方向映射", type = OperationType.QUERY, description = "查询变更历史")
    @Operation(summary = "查询单条变更历史")
    @ApiResponse(responseCode = "200")
    public ApiResult<List<DirMapHistoryResponse>> history(
            @PathVariable String messageType,
            @PathVariable String accessRole) {
        return ApiResult.success(service.history(messageType, accessRole));
    }
}
