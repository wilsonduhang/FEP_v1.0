package com.puchain.fep.web.sysmgmt.config.alert.dto;

import com.puchain.fep.web.sysmgmt.config.alert.domain.AlertFrequency;
import com.puchain.fep.web.sysmgmt.config.alert.domain.NotifyMethod;
import com.puchain.fep.web.sysmgmt.config.alert.domain.SysAlertRule;

import java.time.LocalDateTime;

/**
 * 接口预警规则响应 DTO。
 *
 * <p>参见 PRD v1.3 §5.10.7.2d 接口预警管理（FR-WEB-SYS-CONF-ALERT）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class AlertRuleResponse {

    private String ruleId;
    private Boolean alertEnabled;
    private Integer threshold;
    private String alertEmail;
    private NotifyMethod notifyMethod;
    private AlertFrequency alertFrequency;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /**
     * 从 SysAlertRule Entity 构建响应 DTO。
     *
     * @param entity 预警规则 Entity
     * @return 响应 DTO
     */
    public static AlertRuleResponse from(final SysAlertRule entity) {
        AlertRuleResponse resp = new AlertRuleResponse();
        resp.setRuleId(entity.getRuleId());
        resp.setAlertEnabled(entity.getAlertEnabled());
        resp.setThreshold(entity.getThreshold());
        resp.setAlertEmail(entity.getAlertEmail());
        resp.setNotifyMethod(entity.getNotifyMethod());
        resp.setAlertFrequency(entity.getAlertFrequency());
        resp.setCreateTime(entity.getCreateTime());
        resp.setUpdateTime(entity.getUpdateTime());
        return resp;
    }

    /**
     * 获取规则唯一标识。
     *
     * @return 规则 ID
     */
    public String getRuleId() {
        return ruleId;
    }

    /**
     * 设置规则唯一标识。
     *
     * @param ruleId 规则 ID
     */
    public void setRuleId(final String ruleId) {
        this.ruleId = ruleId;
    }

    /**
     * 获取预警启用状态。
     *
     * @return true 表示启用，false 表示禁用
     */
    public Boolean getAlertEnabled() {
        return alertEnabled;
    }

    /**
     * 设置预警启用状态。
     *
     * @param alertEnabled 是否启用预警
     */
    public void setAlertEnabled(final Boolean alertEnabled) {
        this.alertEnabled = alertEnabled;
    }

    /**
     * 获取预警阈值。
     *
     * @return 预警阈值
     */
    public Integer getThreshold() {
        return threshold;
    }

    /**
     * 设置预警阈值。
     *
     * @param threshold 预警阈值
     */
    public void setThreshold(final Integer threshold) {
        this.threshold = threshold;
    }

    /**
     * 获取通知邮箱。
     *
     * @return 通知邮箱，可能为 null
     */
    public String getAlertEmail() {
        return alertEmail;
    }

    /**
     * 设置通知邮箱。
     *
     * @param alertEmail 通知邮箱
     */
    public void setAlertEmail(final String alertEmail) {
        this.alertEmail = alertEmail;
    }

    /**
     * 获取通知方式。
     *
     * @return 通知方式枚举
     */
    public NotifyMethod getNotifyMethod() {
        return notifyMethod;
    }

    /**
     * 设置通知方式。
     *
     * @param notifyMethod 通知方式枚举
     */
    public void setNotifyMethod(final NotifyMethod notifyMethod) {
        this.notifyMethod = notifyMethod;
    }

    /**
     * 获取告警频率。
     *
     * @return 告警频率枚举
     */
    public AlertFrequency getAlertFrequency() {
        return alertFrequency;
    }

    /**
     * 设置告警频率。
     *
     * @param alertFrequency 告警频率枚举
     */
    public void setAlertFrequency(final AlertFrequency alertFrequency) {
        this.alertFrequency = alertFrequency;
    }

    /**
     * 获取创建时间。
     *
     * @return 创建时间
     */
    public LocalDateTime getCreateTime() {
        return createTime;
    }

    /**
     * 设置创建时间。
     *
     * @param createTime 创建时间
     */
    public void setCreateTime(final LocalDateTime createTime) {
        this.createTime = createTime;
    }

    /**
     * 获取更新时间。
     *
     * @return 更新时间
     */
    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    /**
     * 设置更新时间。
     *
     * @param updateTime 更新时间
     */
    public void setUpdateTime(final LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
