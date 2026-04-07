package com.puchain.fep.web.sysmgmt.config.enterprise.controller;

import com.puchain.fep.common.domain.ApiResult;
import com.puchain.fep.common.domain.PageResult;
import com.puchain.fep.web.sysmgmt.config.enterprise.dto.EnterpriseCreateRequest;
import com.puchain.fep.web.sysmgmt.config.enterprise.dto.EnterpriseResponse;
import com.puchain.fep.web.sysmgmt.config.enterprise.service.SysEnterpriseService;
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

/**
 * 企业主体管理 REST API。
 *
 * <p>提供企业主体 CRUD 接口，支持 USCI 唯一性校验及审核状态过滤。
 * 参见 PRD v1.3 §5.10.7.3 企业主体管理（FR-WEB-SYS-CONF-ENT）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/sys/config/enterprises")
@Tag(name = "17. 系统配置 — 企业主体", description = "企业主体 CRUD，含 USCI 18 位校验")
public class SysEnterpriseController {

    private final SysEnterpriseService enterpriseService;

    /**
     * 构造 SysEnterpriseController。
     *
     * @param enterpriseService 企业主体管理服务
     */
    public SysEnterpriseController(final SysEnterpriseService enterpriseService) {
        this.enterpriseService = enterpriseService;
    }

    /**
     * 搜索企业主体（分页）。
     *
     * @param keyword     关键字（可选，匹配企业名称或 USCI）
     * @param auditStatus 审核状态（可选，PENDING/APPROVED/REJECTED）
     * @param pageNum     页码（1-based，默认 1）
     * @param pageSize    每页大小（默认 10）
     * @return 分页企业主体列表
     */
    @GetMapping
    @OperationLog(module = "企业主体管理", type = OperationType.QUERY, description = "搜索企业主体")
    @Operation(summary = "搜索企业主体", description = "按企业名称/USCI 关键字和审核状态分页搜索")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public ApiResult<PageResult<EnterpriseResponse>> search(
            @Parameter(description = "关键字") @RequestParam(required = false) final String keyword,
            @Parameter(description = "审核状态") @RequestParam(required = false) final String auditStatus,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") final int pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") final int pageSize) {
        return ApiResult.success(enterpriseService.search(keyword, auditStatus, pageNum, pageSize));
    }

    /**
     * 按 ID 查询企业主体。
     *
     * @param enterpriseId 企业主体 ID
     * @return 企业主体详情
     */
    @GetMapping("/{enterpriseId}")
    @OperationLog(module = "企业主体管理", type = OperationType.QUERY, description = "查询企业主体详情")
    @Operation(summary = "查询企业主体详情", description = "按 ID 查询企业主体")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @ApiResponse(responseCode = "404", description = "企业主体不存在")
    public ApiResult<EnterpriseResponse> getById(
            @Parameter(description = "企业主体 ID") @PathVariable final String enterpriseId) {
        return ApiResult.success(enterpriseService.getById(enterpriseId));
    }

    /**
     * 创建企业主体。
     *
     * @param request 创建请求
     * @return 新建企业主体信息
     */
    @PostMapping
    @OperationLog(module = "企业主体管理", type = OperationType.CREATE, description = "创建企业主体")
    @Operation(summary = "创建企业主体", description = "新增企业主体，USCI 不可重复，初始状态为 PENDING")
    @ApiResponse(responseCode = "200", description = "创建成功")
    @ApiResponse(responseCode = "400", description = "参数校验失败")
    @ApiResponse(responseCode = "409", description = "USCI 已存在")
    public ApiResult<EnterpriseResponse> create(
            @Valid @RequestBody final EnterpriseCreateRequest request) {
        return ApiResult.success(enterpriseService.create(request));
    }

    /**
     * 更新企业主体信息（USCI 不可变）。
     *
     * @param enterpriseId 企业主体 ID
     * @param request      更新请求
     * @return 更新后的企业主体信息
     */
    @PutMapping("/{enterpriseId}")
    @OperationLog(module = "企业主体管理", type = OperationType.UPDATE, description = "更新企业主体")
    @Operation(summary = "更新企业主体", description = "修改企业名称及可选字段；USCI 创建后不可变")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @ApiResponse(responseCode = "404", description = "企业主体不存在")
    public ApiResult<EnterpriseResponse> update(
            @Parameter(description = "企业主体 ID") @PathVariable final String enterpriseId,
            @Valid @RequestBody final EnterpriseCreateRequest request) {
        return ApiResult.success(enterpriseService.update(enterpriseId, request));
    }

    /**
     * 删除企业主体。
     *
     * @param enterpriseId 企业主体 ID
     * @return 空响应
     */
    @DeleteMapping("/{enterpriseId}")
    @OperationLog(module = "企业主体管理", type = OperationType.DELETE, description = "删除企业主体")
    @Operation(summary = "删除企业主体", description = "删除指定企业主体")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @ApiResponse(responseCode = "404", description = "企业主体不存在")
    public ApiResult<Void> delete(
            @Parameter(description = "企业主体 ID") @PathVariable final String enterpriseId) {
        enterpriseService.delete(enterpriseId);
        return ApiResult.success();
    }
}
