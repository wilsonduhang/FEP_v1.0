package com.puchain.fep.web.sysmgmt.config.alert.dto;

import com.puchain.fep.web.sysmgmt.config.alert.domain.AlertFrequency;
import com.puchain.fep.web.sysmgmt.config.alert.domain.NotifyMethod;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

/**
 * 接口预警规则更新请求 DTO。
 *
 * <p>参见 PRD v1.3 §5.10.7.2d 接口预警管理（FR-WEB-SYS-CONF-ALERT）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class AlertRuleUpdateRequest {

    @NotNull(message = "alertEnabled 不能为空")
    private Boolean alertEnabled;

    @NotNull(message = "threshold 不能为空")
    @Min(value = 0, message = "threshold 不能为负数")
    private Integer threshold;

    @Email(message = "alertEmail 格式不合法")
    @Size(max = 200, message = "alertEmail 不能超过 200 个字符")
    private String alertEmail;

    @NotEmpty(message = "notifyMethods 不能为空")
    private Set<NotifyMethod> notifyMethods;

    @Size(max = 50, message = "alertPhone 不能超过 50 个字符")
    private String alertPhone;

    @NotNull(message = "alertFrequency 不能为空")
    private AlertFrequency alertFrequency;

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
     * @return 预警阈值（非负整数）
     */
    public Integer getThreshold() {
        return threshold;
    }

    /**
     * 设置预警阈值。
     *
     * @param threshold 预警阈值（非负整数）
     */
    public void setThreshold(final Integer threshold) {
        this.threshold = threshold;
    }

    /**
     * 获取通知邮箱。
     *
     * @return 通知邮箱，可为 null 或空字符串表示不通知邮件
     */
    public String getAlertEmail() {
        return alertEmail;
    }

    /**
     * 设置通知邮箱。
     *
     * @param alertEmail 通知邮箱（可为 null）
     */
    public void setAlertEmail(final String alertEmail) {
        this.alertEmail = alertEmail;
    }

    /**
     * 获取启用的通知渠道集合。
     *
     * @return 渠道集合
     */
    public Set<NotifyMethod> getNotifyMethods() {
        return notifyMethods;
    }

    /**
     * 设置启用的通知渠道集合。
     *
     * @param notifyMethods 渠道集合
     */
    public void setNotifyMethods(final Set<NotifyMethod> notifyMethods) {
        this.notifyMethods = notifyMethods;
    }

    /**
     * 获取 SMS 告警收件手机号。
     *
     * @return 手机号，可能为 null
     */
    public String getAlertPhone() {
        return alertPhone;
    }

    /**
     * 设置 SMS 告警收件手机号。
     *
     * @param alertPhone 手机号（可为 null）
     */
    public void setAlertPhone(final String alertPhone) {
        this.alertPhone = alertPhone;
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
}
