package com.puchain.fep.web.sysmgmt.config.alert.controller;

import com.puchain.fep.common.domain.ApiResult;
import com.puchain.fep.web.sysmgmt.config.alert.dto.AlertRuleResponse;
import com.puchain.fep.web.sysmgmt.config.alert.dto.AlertRuleUpdateRequest;
import com.puchain.fep.web.sysmgmt.config.alert.service.SysAlertRuleService;
import com.puchain.fep.web.sysmgmt.log.annotation.OperationLog;
import com.puchain.fep.web.sysmgmt.log.domain.OperationType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 接口预警管理 REST API。
 *
 * <p>t_sys_alert_rule 为单条配置记录，仅提供 GET（读取）和 PUT（更新）两个端点，
 * 不支持新增或删除。参见 PRD v1.3 §5.10.7.2d 接口预警管理（FR-WEB-SYS-CONF-ALERT）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/sys/config/alert-rules")
@Tag(name = "15. 接口预警管理", description = "接口预警规则单表单配置（读取/更新）")
public class SysAlertRuleController {

    private final SysAlertRuleService alertRuleService;

    /**
     * 构造 SysAlertRuleController。
     *
     * @param alertRuleService 预警规则服务
     */
    public SysAlertRuleController(final SysAlertRuleService alertRuleService) {
        this.alertRuleService = alertRuleService;
    }

    /**
     * 查询预警规则配置。
     *
     * @return 预警规则响应 DTO
     */
    @GetMapping
    @OperationLog(module = "接口预警", type = OperationType.QUERY, description = "查询预警规则配置")
    @Operation(summary = "查询预警规则配置", description = "读取系统唯一的接口预警规则配置")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @ApiResponse(responseCode = "404", description = "预警规则配置不存在（BIZ_5001）")
    public ApiResult<AlertRuleResponse> getRule() {
        return ApiResult.success(alertRuleService.getRule());
    }

    /**
     * 更新预警规则配置。
     *
     * @param request 更新请求 DTO
     * @return 更新后的预警规则响应 DTO
     */
    @PutMapping
    @OperationLog(module = "接口预警", type = OperationType.UPDATE, description = "更新预警规则配置")
    @Operation(summary = "更新预警规则配置", description = "更新系统唯一的接口预警规则配置")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @ApiResponse(responseCode = "400", description = "参数校验失败（格式不合法或必填字段为空）")
    @ApiResponse(responseCode = "404", description = "预警规则配置不存在（BIZ_5001）")
    public ApiResult<AlertRuleResponse> updateRule(
            @Valid @RequestBody final AlertRuleUpdateRequest request) {
        return ApiResult.success(alertRuleService.updateRule(request));
    }
}
