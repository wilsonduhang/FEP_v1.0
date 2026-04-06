package com.puchain.fep.web.sysmgmt.help.controller;

import com.puchain.fep.common.domain.ApiResult;
import com.puchain.fep.web.sysmgmt.help.dto.HelpContentResponse;
import com.puchain.fep.web.sysmgmt.help.dto.HelpCreateRequest;
import com.puchain.fep.web.sysmgmt.help.dto.HelpUpdateRequest;
import com.puchain.fep.web.sysmgmt.help.service.SysHelpContentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
 * 帮助面板 REST API。
 *
 * <p>提供页面级上下文帮助内容的查询、新增与更新接口。
 * 按页面编码动态返回对应帮助信息，最多 4 条（PRD §5.10.8）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/sys/help")
@Tag(name = "08. 帮助面板", description = "页面级上下文帮助内容")
public class SysHelpContentController {

    private final SysHelpContentService helpService;

    /**
     * 构造 SysHelpContentController。
     *
     * @param helpService 帮助内容服务
     */
    public SysHelpContentController(final SysHelpContentService helpService) {
        this.helpService = helpService;
    }

    /**
     * 按页面编码查询帮助内容。
     *
     * <p>仅返回状态为 ACTIVE 的帮助条目，最多 4 条，按 sortOrder 升序排列。</p>
     *
     * @param pageCode 页面编码（如 sys-user、sys-role）
     * @return 帮助内容列表
     */
    @GetMapping
    @Operation(summary = "按页面编码查询帮助内容",
            description = "返回指定页面的启用状态帮助条目，最多 4 条，按排序值升序")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public ApiResult<List<HelpContentResponse>> findByPageCode(
            @Parameter(description = "页面编码（如 sys-user、sys-role）", required = true)
            @RequestParam final String pageCode) {
        return ApiResult.success(helpService.findByPageCode(pageCode));
    }

    /**
     * 新增帮助内容条目。
     *
     * <p>新建条目 helpStatus 默认为 ACTIVE，sortOrder 自动追加。</p>
     *
     * @param request 帮助内容创建请求
     * @return 已创建的帮助内容信息
     */
    @PostMapping
    @Operation(summary = "新增帮助内容", description = "为指定页面新增一条帮助内容，状态默认为 ACTIVE")
    @ApiResponse(responseCode = "200", description = "新增成功")
    @ApiResponse(responseCode = "400", description = "参数校验失败")
    public ApiResult<HelpContentResponse> create(
            @Valid @RequestBody final HelpCreateRequest request) {
        return ApiResult.success(helpService.create(
                request.getPageCode(),
                request.getTitle(),
                request.getSummary(),
                request.getContent()));
    }

    /**
     * 更新帮助内容条目（局部更新）。
     *
     * <p>仅更新请求体中非 null 的字段；帮助条目不存在时返回 BIZ_5001 错误。</p>
     *
     * @param helpId  帮助 ID
     * @param request 帮助内容更新请求
     * @return 更新后的帮助内容信息
     */
    @PutMapping("/{helpId}")
    @Operation(summary = "更新帮助内容", description = "局部更新指定帮助条目，仅更新非 null 字段")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @ApiResponse(responseCode = "400", description = "帮助条目不存在或参数校验失败")
    public ApiResult<HelpContentResponse> update(
            @Parameter(description = "帮助 ID") @PathVariable final String helpId,
            @Valid @RequestBody final HelpUpdateRequest request) {
        return ApiResult.success(helpService.update(
                helpId,
                request.getTitle(),
                request.getSummary(),
                request.getContent()));
    }
}
