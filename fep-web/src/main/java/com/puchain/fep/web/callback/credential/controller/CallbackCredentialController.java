package com.puchain.fep.web.callback.credential.controller;

import com.puchain.fep.common.domain.ApiResult;
import com.puchain.fep.web.callback.credential.dto.CallbackCredentialCreateRequest;
import com.puchain.fep.web.callback.credential.dto.CallbackCredentialResponse;
import com.puchain.fep.web.callback.credential.dto.CallbackCredentialSweepResponse;
import com.puchain.fep.web.callback.credential.dto.CallbackCredentialUpdateRequest;
import com.puchain.fep.web.callback.credential.service.CallbackCredentialAdminService;
import com.puchain.fep.web.sysmgmt.log.annotation.OperationLog;
import com.puchain.fep.web.sysmgmt.log.domain.OperationType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 回调凭证管理 REST API（密文不回显）。
 *
 * <p>提供凭证 CRUD：新建/查询/列表/局部更新/删除。明文凭证经 server 端 SM4 加密落库，
 * 响应仅含元数据 + 配置标记，绝不回显任何密文。仅管理员（ROLE_ADMIN）可访问。
 * 参见 PRD v1.3 §5.5.3 凭证配置（FR-INFRA-CALLBACK-CREDENTIAL）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/callback/credentials")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "回调凭证管理", description = "接口模式回调凭证 CRUD（密文不回显）")
public class CallbackCredentialController {

    private final CallbackCredentialAdminService service;

    /**
     * 构造凭证管理控制器。
     *
     * @param service 凭证管理服务
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Spring-managed singleton stored by reference per container contract")
    public CallbackCredentialController(final CallbackCredentialAdminService service) {
        this.service = service;
    }

    /**
     * 新建凭证。
     *
     * @param req 新建请求（明文凭证 server 端加密）
     * @return 不含密文的凭证响应
     */
    @PostMapping
    @OperationLog(module = "回调凭证管理", type = OperationType.CREATE, description = "新建回调凭证")
    @Operation(summary = "新建凭证", description = "明文凭证 server 端 SM4 加密落库，响应不回显密文")
    @ApiResponse(responseCode = "200", description = "创建成功")
    @ApiResponse(responseCode = "400", description = "参数校验失败")
    @ApiResponse(responseCode = "409", description = "接口已存在凭证")
    public ApiResult<CallbackCredentialResponse> create(@Valid @RequestBody final CallbackCredentialCreateRequest req) {
        return ApiResult.success(service.create(req));
    }

    /**
     * 查询凭证（不回显密文）。
     *
     * @param interfaceId 接口 ID
     * @return 不含密文的凭证响应
     */
    @GetMapping("/{interfaceId}")
    @OperationLog(module = "回调凭证管理", type = OperationType.QUERY, description = "查询回调凭证")
    @Operation(summary = "查询凭证", description = "返回元数据 + 配置标记，不回显密文")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @ApiResponse(responseCode = "404", description = "凭证不存在")
    public ApiResult<CallbackCredentialResponse> get(
            @Parameter(description = "输出接口 ID") @PathVariable final String interfaceId) {
        return ApiResult.success(service.get(interfaceId));
    }

    /**
     * 列出全部凭证（不回显密文）。
     *
     * @return 凭证响应列表
     */
    @GetMapping
    @OperationLog(module = "回调凭证管理", type = OperationType.QUERY, description = "回调凭证列表")
    @Operation(summary = "凭证列表", description = "返回全部凭证元数据，不回显密文")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public ApiResult<List<CallbackCredentialResponse>> list() {
        return ApiResult.success(service.list());
    }

    /**
     * 局部更新凭证（字段空=保留原值，密文字段非空=重新加密轮换）。
     *
     * @param interfaceId 接口 ID
     * @param req         更新请求
     * @return 不含密文的凭证响应
     */
    @PutMapping("/{interfaceId}")
    @OperationLog(module = "回调凭证管理", type = OperationType.UPDATE, description = "更新回调凭证")
    @Operation(summary = "更新凭证", description = "partial — 字段空=保留原值，密文字段非空=轮换，更新后清空 token 缓存")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @ApiResponse(responseCode = "404", description = "凭证不存在")
    public ApiResult<CallbackCredentialResponse> update(
            @Parameter(description = "输出接口 ID") @PathVariable final String interfaceId,
            @Valid @RequestBody final CallbackCredentialUpdateRequest req) {
        return ApiResult.success(service.update(interfaceId, req));
    }

    /**
     * 轮换凭证密钥（解密旧 keyId → 重加密当前活跃 SM4 主密钥）。
     *
     * @param interfaceId 接口 ID
     * @return 不含密文的凭证响应
     */
    @PostMapping("/{interfaceId}/rotate-key")
    @OperationLog(module = "回调凭证管理", type = OperationType.UPDATE, description = "轮换凭证密钥")
    @Operation(summary = "轮换凭证密钥", description = "用旧 keyId 解密后以当前活跃 SM4 主密钥重加密落库")
    @ApiResponse(responseCode = "200", description = "轮换成功")
    @ApiResponse(responseCode = "404", description = "凭证不存在")
    public ApiResult<CallbackCredentialResponse> rotateKey(
            @Parameter(description = "输出接口 ID") @PathVariable final String interfaceId) {
        return ApiResult.success(service.rotateKey(interfaceId));
    }

    /**
     * 批量迁移 legacy 明文凭证（冷接口收口；运维 DB 备份后触发）。
     *
     * @return 迁移/失败/剩余计数（不回显凭证内容）
     */
    @PostMapping("/migrate-legacy")
    @OperationLog(module = "回调凭证管理", type = OperationType.UPDATE,
            description = "批量迁移 legacy 明文凭证")
    @Operation(summary = "批量迁移 legacy 明文凭证",
            description = "逐行 REQUIRES_NEW 重加密为活跃 SM4 主密钥；单行失败不阻断，计数披露。"
                    + "迁移有损回退（回滚窗口=首行迁移前），触发前须 DB 备份")
    @ApiResponse(responseCode = "200", description = "扫描完成（含失败计数）")
    public ApiResult<CallbackCredentialSweepResponse> migrateLegacy() {
        return ApiResult.success(service.migrateLegacy());
    }

    /**
     * 删除凭证（幂等）。
     *
     * @param interfaceId 接口 ID
     * @return 空结果
     */
    @DeleteMapping("/{interfaceId}")
    @OperationLog(module = "回调凭证管理", type = OperationType.DELETE, description = "删除回调凭证")
    @Operation(summary = "删除凭证", description = "删除并清空 token 缓存，不存在时静默成功")
    @ApiResponse(responseCode = "200", description = "删除成功")
    public ApiResult<Void> delete(
            @Parameter(description = "输出接口 ID") @PathVariable final String interfaceId) {
        service.delete(interfaceId);
        return ApiResult.success();
    }
}
