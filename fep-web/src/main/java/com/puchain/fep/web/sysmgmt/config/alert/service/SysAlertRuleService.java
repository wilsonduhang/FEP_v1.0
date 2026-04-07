package com.puchain.fep.web.sysmgmt.config.alert.service;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.web.sysmgmt.config.alert.domain.SysAlertRule;
import com.puchain.fep.web.sysmgmt.config.alert.dto.AlertRuleResponse;
import com.puchain.fep.web.sysmgmt.config.alert.dto.AlertRuleUpdateRequest;
import com.puchain.fep.web.sysmgmt.config.alert.repository.SysAlertRuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 接口预警规则服务。
 *
 * <p>t_sys_alert_rule 为单条配置记录，提供读取与更新操作。
 * 参见 PRD v1.3 §5.10.7.2d 接口预警管理（FR-WEB-SYS-CONF-ALERT）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Service
public class SysAlertRuleService {

    private static final Logger LOG = LoggerFactory.getLogger(SysAlertRuleService.class);

    private final SysAlertRuleRepository alertRuleRepository;

    /**
     * 构造 SysAlertRuleService。
     *
     * @param alertRuleRepository 预警规则 Repository
     */
    public SysAlertRuleService(final SysAlertRuleRepository alertRuleRepository) {
        this.alertRuleRepository = alertRuleRepository;
    }

    /**
     * 查询单条预警规则配置。
     *
     * <p>t_sys_alert_rule 为单条配置记录，若表中无数据则抛出 BIZ_5001。</p>
     *
     * @return 预警规则响应 DTO
     * @throws FepBusinessException 当表中无配置记录时（BIZ_5001）
     */
    @Transactional(readOnly = true)
    public AlertRuleResponse getRule() {
        List<SysAlertRule> rules = alertRuleRepository.findAll();
        if (rules.isEmpty()) {
            throw new FepBusinessException(FepErrorCode.BIZ_5001, "预警规则配置不存在，请联系系统管理员初始化数据");
        }
        SysAlertRule rule = rules.get(0);
        LOG.info("Alert rule retrieved: ruleId={}", rule.getRuleId());
        return AlertRuleResponse.from(rule);
    }

    /**
     * 更新单条预警规则配置。
     *
     * <p>查找表中唯一的配置记录并更新字段，若不存在则抛出 BIZ_5001。</p>
     *
     * @param request 更新请求 DTO
     * @return 更新后的预警规则响应 DTO
     * @throws FepBusinessException 当表中无配置记录时（BIZ_5001）
     */
    @Transactional
    public AlertRuleResponse updateRule(final AlertRuleUpdateRequest request) {
        List<SysAlertRule> rules = alertRuleRepository.findAll();
        if (rules.isEmpty()) {
            throw new FepBusinessException(FepErrorCode.BIZ_5001, "预警规则配置不存在，请联系系统管理员初始化数据");
        }
        SysAlertRule rule = rules.get(0);
        rule.setAlertEnabled(request.getAlertEnabled());
        rule.setThreshold(request.getThreshold());
        rule.setAlertEmail(request.getAlertEmail());
        rule.setNotifyMethod(request.getNotifyMethod());
        rule.setAlertFrequency(request.getAlertFrequency());
        rule.setUpdateTime(LocalDateTime.now());
        alertRuleRepository.save(rule);
        LOG.info("Alert rule updated: ruleId={}, alertEnabled={}, threshold={}",
                rule.getRuleId(), rule.getAlertEnabled(), rule.getThreshold());
        return AlertRuleResponse.from(rule);
    }
}
