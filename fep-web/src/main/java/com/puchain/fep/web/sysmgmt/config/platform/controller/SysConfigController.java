package com.puchain.fep.web.sysmgmt.config.platform.controller;

import com.puchain.fep.common.domain.ApiResult;
import com.puchain.fep.web.sysmgmt.config.platform.dto.ConfigBatchUpdateRequest;
import com.puchain.fep.web.sysmgmt.config.platform.dto.ConfigGroupResponse;
import com.puchain.fep.web.sysmgmt.config.platform.service.SysConfigService;
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
import org.springframework.web.bind.annotation.RestController;

/**
 * 通用系统配置 REST API。
 *
 * <p>提供 key-value 配置的按组读取和批量更新。
 * 覆盖 §5.10.7.1 平台基础设置 + §5.10.7.4 其他系统配置。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/sys/config")
@Tag(name = "09. 系统配置", description = "平台基础设置 / 系统参数 / 证书元数据")
public class SysConfigController {

    private final SysConfigService configService;

    /**
     * 构造 SysConfigController。
     *
     * @param configService 配置服务
     */
    public SysConfigController(final SysConfigService configService) {
        this.configService = configService;
    }

    /**
     * 查询配置组。
     *
     * @param group 配置分组（PLATFORM / SYSTEM / CERT）
     * @return 配置组数据
     */
    @GetMapping("/{group}")
    @OperationLog(module = "系统配置", type = OperationType.QUERY, description = "查询配置组")
    @Operation(summary = "查询配置组", description = "按分组查询所有配置项（PLATFORM / SYSTEM / CERT）")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public ApiResult<ConfigGroupResponse> getByGroup(
            @Parameter(description = "配置分组，如 PLATFORM、SYSTEM、CERT")
            @PathVariable final String group) {
        return ApiResult.success(configService.getByGroup(group));
    }

    /**
     * 批量更新配置组。
     *
     * @param group   配置分组
     * @param request 批量更新请求（key-value Map）
     * @return 更新后的配置组数据
     */
    @PutMapping("/{group}")
    @OperationLog(module = "系统配置", type = OperationType.UPDATE, description = "批量更新配置组")
    @Operation(summary = "批量更新配置组", description = "批量更新指定分组下的配置值，仅更新请求中提供的 key")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @ApiResponse(responseCode = "400", description = "参数校验失败（格式不合法或字段为空）")
    public ApiResult<ConfigGroupResponse> batchUpdate(
            @Parameter(description = "配置分组，如 PLATFORM、SYSTEM、CERT")
            @PathVariable final String group,
            @Valid @RequestBody final ConfigBatchUpdateRequest request) {
        return ApiResult.success(configService.batchUpdate(group, request));
    }
}
