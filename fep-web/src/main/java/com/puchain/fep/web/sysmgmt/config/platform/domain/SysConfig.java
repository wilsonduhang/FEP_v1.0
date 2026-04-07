package com.puchain.fep.web.sysmgmt.config.platform.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 通用系统配置 Entity，映射 t_sys_config 表。
 *
 * <p>采用 config_group + config_key 二维 key-value 模型，
 * 支撑平台基础设置（§5.10.7.1）和其他系统配置（§5.10.7.4）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Entity
@Table(name = "t_sys_config")
public class SysConfig {

    @Id
    @Column(name = "config_id", length = 32)
    private String configId;

    @Column(name = "config_group", nullable = false, length = 50)
    private String configGroup;

    @Column(name = "config_key", nullable = false, length = 100)
    private String configKey;

    @Column(name = "config_value", columnDefinition = "TEXT")
    private String configValue;

    @Column(name = "config_desc", length = 200)
    private String configDesc;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    /**
     * 无参构造方法（JPA 要求）。
     */
    public SysConfig() {
        /* for JPA */
    }

    // ===== Getters =====

    /**
     * 获取配置唯一标识。
     *
     * @return 配置 ID (UUID 32位)
     */
    public String getConfigId() {
        return configId;
    }

    /**
     * 获取配置分组。
     *
     * @return 配置分组（如 PLATFORM / SYSTEM / CERT）
     */
    public String getConfigGroup() {
        return configGroup;
    }

    /**
     * 获取配置键。
     *
     * @return 配置键
     */
    public String getConfigKey() {
        return configKey;
    }

    /**
     * 获取配置值。
     *
     * @return 配置值，可能为 null
     */
    public String getConfigValue() {
        return configValue;
    }

    /**
     * 获取配置描述。
     *
     * @return 配置描述，可能为 null
     */
    public String getConfigDesc() {
        return configDesc;
    }

    /**
     * 获取排序号。
     *
     * @return 排序号
     */
    public Integer getSortOrder() {
        return sortOrder;
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
     * 设置配置唯一标识。
     *
     * @param configId 配置 ID (UUID 32位)
     */
    public void setConfigId(String configId) {
        this.configId = configId;
    }

    /**
     * 设置配置分组。
     *
     * @param configGroup 配置分组
     */
    public void setConfigGroup(String configGroup) {
        this.configGroup = configGroup;
    }

    /**
     * 设置配置键。
     *
     * @param configKey 配置键
     */
    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    /**
     * 设置配置值。
     *
     * @param configValue 配置值
     */
    public void setConfigValue(String configValue) {
        this.configValue = configValue;
    }

    /**
     * 设置配置描述。
     *
     * @param configDesc 配置描述
     */
    public void setConfigDesc(String configDesc) {
        this.configDesc = configDesc;
    }

    /**
     * 设置排序号。
     *
     * @param sortOrder 排序号
     */
    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    /**
     * 设置创建时间。
     *
     * @param createTime 创建时间
     */
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    /**
     * 设置更新时间。
     *
     * @param updateTime 更新时间
     */
    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
