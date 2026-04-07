package com.puchain.fep.web.sysmgmt.config.enterprise.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 企业精准查询配置 Entity，映射 t_sys_enterprise_query_config 表。
 *
 * <p>每个企业最多一条查询配置记录（enterprise_id UNIQUE）。
 * 参见 PRD v1.3 §5.10.7.3 企业主体管理（Tab 3 精准查询配置）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Entity
@Table(name = "t_sys_enterprise_query_config")
public class SysEnterpriseQueryConfig {

    @Id
    @Column(name = "id", length = 32)
    private String id;

    @Column(name = "enterprise_id", nullable = false, length = 32, unique = true)
    private String enterpriseId;

    @Column(name = "query_type", nullable = false, length = 50)
    private String queryType;

    @Column(name = "query_params", columnDefinition = "TEXT")
    private String queryParams;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    /**
     * 无参构造方法（JPA 要求）。
     */
    public SysEnterpriseQueryConfig() {
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
     * 获取查询类型。
     *
     * @return 查询类型（最大 50 字符）
     */
    public String getQueryType() {
        return queryType;
    }

    /**
     * 设置查询类型。
     *
     * @param queryType 查询类型
     */
    public void setQueryType(final String queryType) {
        this.queryType = queryType;
    }

    /**
     * 获取查询参数 JSON。
     *
     * @return 查询参数（可为 null）
     */
    public String getQueryParams() {
        return queryParams;
    }

    /**
     * 设置查询参数 JSON。
     *
     * @param queryParams 查询参数
     */
    public void setQueryParams(final String queryParams) {
        this.queryParams = queryParams;
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
