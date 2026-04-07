package com.puchain.fep.web.sysmgmt.config.enterprise.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 企业业务信息关联 Entity，映射 t_sys_enterprise_biz 表。
 *
 * <p>记录企业主体与业务类型的关联关系。
 * 参见 PRD v1.3 §5.10.7.3 企业主体管理（Tab 2 业务信息）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Entity
@Table(name = "t_sys_enterprise_biz")
public class SysEnterpriseBiz {

    @Id
    @Column(name = "id", length = 32)
    private String id;

    @Column(name = "enterprise_id", nullable = false, length = 32)
    private String enterpriseId;

    @Column(name = "business_type_id", nullable = false, length = 32)
    private String businessTypeId;

    @Column(name = "config_json", columnDefinition = "TEXT")
    private String configJson;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    /**
     * 无参构造方法（JPA 要求）。
     */
    public SysEnterpriseBiz() {
        /* for JPA */
    }

    /**
     * 获取记录唯一标识。
     *
     * @return 记录 ID（UUID 32位）
     */
    public String getId() {
        return id;
    }

    /**
     * 设置记录唯一标识。
     *
     * @param id 记录 ID
     */
    public void setId(final String id) {
        this.id = id;
    }

    /**
     * 获取企业主体 ID。
     *
     * @return 企业主体 ID
     */
    public String getEnterpriseId() {
        return enterpriseId;
    }

    /**
     * 设置企业主体 ID。
     *
     * @param enterpriseId 企业主体 ID
     */
    public void setEnterpriseId(final String enterpriseId) {
        this.enterpriseId = enterpriseId;
    }

    /**
     * 获取业务类型 ID。
     *
     * @return 业务类型 ID
     */
    public String getBusinessTypeId() {
        return businessTypeId;
    }

    /**
     * 设置业务类型 ID。
     *
     * @param businessTypeId 业务类型 ID
     */
    public void setBusinessTypeId(final String businessTypeId) {
        this.businessTypeId = businessTypeId;
    }

    /**
     * 获取配置 JSON。
     *
     * @return 配置 JSON（可为 null）
     */
    public String getConfigJson() {
        return configJson;
    }

    /**
     * 设置配置 JSON。
     *
     * @param configJson 配置 JSON
     */
    public void setConfigJson(final String configJson) {
        this.configJson = configJson;
    }

    /**
     * 获取状态。
     *
     * @return 状态（ACTIVE/INACTIVE）
     */
    public String getStatus() {
        return status;
    }

    /**
     * 设置状态。
     *
     * @param status 状态
     */
    public void setStatus(final String status) {
        this.status = status;
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
}
