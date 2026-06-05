package com.puchain.fep.web.sysmgmt.config.alert.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

/**
 * 接口预警规则 Entity，映射 t_sys_alert_rule 表。
 *
 * <p>该表为单条配置记录设计，系统初始化时由 V6 迁移脚本插入唯一的默认行。
 * 参见 PRD v1.3 §5.10.7.2d 接口预警管理（FR-WEB-SYS-CONF-ALERT）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Entity
@Table(name = "t_sys_alert_rule")
public class SysAlertRule {

    @Id
    @Column(name = "rule_id", length = 32)
    private String ruleId;

    @Column(name = "alert_enabled", nullable = false)
    private Boolean alertEnabled;

    @Column(name = "threshold", nullable = false)
    private Integer threshold;

    @Column(name = "alert_email", length = 200)
    private String alertEmail;

    @Convert(converter = NotifyMethodSetConverter.class)
    @Column(name = "notify_methods", nullable = false, length = 60)
    private Set<NotifyMethod> notifyMethods = new TreeSet<>(Comparator.comparing(Enum::name));

    @Column(name = "alert_phone", length = 50)
    private String alertPhone;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_frequency", nullable = false, length = 20)
    private AlertFrequency alertFrequency;

    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    /**
     * 无参构造方法（JPA 要求）。
     */
    public SysAlertRule() {
        /* for JPA */
    }

    // ===== Getters =====

    /**
     * 获取规则唯一标识。
     *
     * @return 规则 ID (UUID 32位)
     */
    public String getRuleId() {
        return ruleId;
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
     * 获取预警阈值。
     *
     * @return 预警阈值（非负整数）
     */
    public Integer getThreshold() {
        return threshold;
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
     * 获取启用的通知渠道集合。
     *
     * @return 渠道集合（非 null，可能为空）
     */
    public Set<NotifyMethod> getNotifyMethods() {
        return notifyMethods;
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
     * 获取告警频率。
     *
     * @return 告警频率枚举
     */
    public AlertFrequency getAlertFrequency() {
        return alertFrequency;
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
     * 获取更新时间。
     *
     * @return 更新时间
     */
    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    // ===== Setters =====

    /**
     * 设置规则唯一标识。
     *
     * @param ruleId 规则 ID
     */
    public void setRuleId(final String ruleId) {
        this.ruleId = ruleId;
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
     * 设置预警阈值。
     *
     * @param threshold 预警阈值（非负整数）
     */
    public void setThreshold(final Integer threshold) {
        this.threshold = threshold;
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
     * 设置启用的通知渠道集合。
     *
     * @param notifyMethods 渠道集合（非 null）
     */
    public void setNotifyMethods(final Set<NotifyMethod> notifyMethods) {
        this.notifyMethods = notifyMethods;
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
     * 设置告警频率。
     *
     * @param alertFrequency 告警频率枚举
     */
    public void setAlertFrequency(final AlertFrequency alertFrequency) {
        this.alertFrequency = alertFrequency;
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
     * 设置更新时间。
     *
     * @param updateTime 更新时间
     */
    public void setUpdateTime(final LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
