package com.puchain.fep.web.submission.scene.controller;

import com.puchain.fep.common.domain.ApiResult;
import com.puchain.fep.common.domain.PageResult;
import com.puchain.fep.web.submission.scene.dto.SceneCreateRequest;
import com.puchain.fep.web.submission.scene.dto.SceneResponse;
import com.puchain.fep.web.submission.scene.service.SubBusinessSceneService;
import com.puchain.fep.web.sysmgmt.log.annotation.OperationLog;
import com.puchain.fep.web.sysmgmt.log.domain.OperationType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 业务场景管理 REST Controller。
 *
 * <p>提供业务场景 CRUD 及状态切换接口。
 * 参见 PRD v1.3 §5.5.4 业务场景管理（FR-WEB-SUB-SCENE）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/submission/scenes")
@Tag(name = "业务场景管理", description = "PRD §5.5.4 配置报文业务场景和数据推送规则")
public class SubBusinessSceneController {

    private final SubBusinessSceneService sceneService;

    /**
     * 构造 SubBusinessSceneController。
     *
     * @param sceneService 业务场景管理服务
     */
    public SubBusinessSceneController(final SubBusinessSceneService sceneService) {
        this.sceneService = sceneService;
    }

    /**
     * 搜索业务场景（分页）。
     *
     * @param keyword        关键字（可选，匹配场景名称）
     * @param businessTypeId 业务类型 ID（可选，精确过滤）
     * @param pageNum        页码（1-based，默认 1）
     * @param pageSize       每页大小（默认 10）
     * @return 分页业务场景列表
     */
    @GetMapping
    @OperationLog(module = "业务场景管理", type = OperationType.QUERY, description = "搜索业务场景")
    @Operation(summary = "搜索业务场景", description = "按场景名称模糊搜索，可按业务类型过滤，分页返回")
    @ApiResponse(responseCode = "200", description = "搜索成功")
    public ApiResult<PageResult<SceneResponse>> search(
            @Parameter(description = "场景名称关键字")
            @RequestParam(required = false) final String keyword,
            @Parameter(description = "业务类型 ID（精确过滤）")
            @RequestParam(required = false) final String businessTypeId,
            @Parameter(description = "页码（从1开始）")
            @RequestParam(defaultValue = "1") final int pageNum,
            @Parameter(description = "每页条数")
            @RequestParam(defaultValue = "10") final int pageSize) {
        return ApiResult.success(
                sceneService.search(keyword, businessTypeId, pageNum, pageSize));
    }

    /**
     * 获取业务场景详情。
     *
     * @param sceneId 场景 ID
     * @return 业务场景详情
     */
    @GetMapping("/{sceneId}")
    @OperationLog(module = "业务场景管理", type = OperationType.QUERY, description = "查询业务场景详情")
    @Operation(summary = "业务场景详情", description = "按 ID 查询业务场景")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public ApiResult<SceneResponse> getById(
            @Parameter(description = "场景 ID") @PathVariable final String sceneId) {
        return ApiResult.success(sceneService.getById(sceneId));
    }

    /**
     * 新增业务场景。
     *
     * @param request 创建请求
     * @return 新建业务场景信息
     */
    @PostMapping
    @OperationLog(module = "业务场景管理", type = OperationType.CREATE, description = "新增业务场景")
    @Operation(summary = "新增业务场景", description = "创建新的业务场景配置")
    @ApiResponse(responseCode = "200", description = "创建成功")
    @ApiResponse(responseCode = "400", description = "参数校验失败")
    @ApiResponse(responseCode = "409", description = "名称已存在")
    public ApiResult<SceneResponse> create(
            @Valid @RequestBody final SceneCreateRequest request) {
        return ApiResult.success(sceneService.create(request));
    }

    /**
     * 编辑业务场景。
     *
     * @param sceneId 场景 ID
     * @param request 更新请求
     * @return 更新后的业务场景信息
     */
    @PutMapping("/{sceneId}")
    @OperationLog(module = "业务场景管理", type = OperationType.UPDATE, description = "编辑业务场景")
    @Operation(summary = "编辑业务场景", description = "修改业务场景配置")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @ApiResponse(responseCode = "400", description = "参数校验失败")
    @ApiResponse(responseCode = "404", description = "场景不存在")
    @ApiResponse(responseCode = "409", description = "名称已存在")
    public ApiResult<SceneResponse> update(
            @Parameter(description = "场景 ID") @PathVariable final String sceneId,
            @Valid @RequestBody final SceneCreateRequest request) {
        return ApiResult.success(sceneService.update(sceneId, request));
    }

    /**
     * 切换业务场景状态（启用/停用）。
     *
     * @param sceneId 场景 ID
     * @return 更新后的业务场景信息
     */
    @PatchMapping("/{sceneId}/status")
    @OperationLog(module = "业务场景管理", type = OperationType.UPDATE,
            description = "切换业务场景状态")
    @Operation(summary = "启用/停用业务场景", description = "ENABLED↔DISABLED 切换")
    @ApiResponse(responseCode = "200", description = "切换成功")
    @ApiResponse(responseCode = "404", description = "场景不存在")
    public ApiResult<SceneResponse> toggleStatus(
            @Parameter(description = "场景 ID") @PathVariable final String sceneId) {
        return ApiResult.success(sceneService.toggleStatus(sceneId));
    }

    /**
     * 删除业务场景。
     *
     * @param sceneId 场景 ID
     * @return 空响应
     */
    @DeleteMapping("/{sceneId}")
    @OperationLog(module = "业务场景管理", type = OperationType.DELETE, description = "删除业务场景")
    @Operation(summary = "删除业务场景", description = "物理删除业务场景")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @ApiResponse(responseCode = "404", description = "场景不存在")
    public ApiResult<Void> delete(
            @Parameter(description = "场景 ID") @PathVariable final String sceneId) {
        sceneService.delete(sceneId);
        return ApiResult.success();
    }
}
