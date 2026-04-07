package com.puchain.fep.web.sysmgmt.config.businesstype.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 业务类型 Entity，映射 t_sys_business_type 表。
 *
 * <p>参见 PRD v1.3 §5.10.7.2a 业务类型管理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Entity
@Table(name = "t_sys_business_type")
@EntityListeners(AuditingEntityListener.class)
public class SysBusinessType {

    @Id
    @Column(name = "type_id", length = 32)
    private String typeId;

    @Column(name = "type_name", nullable = false, length = 100)
    private String typeName;

    @Column(name = "type_code", nullable = false, length = 50, unique = true)
    private String typeCode;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_status", nullable = false, length = 20)
    private BusinessTypeStatus typeStatus;

    @CreatedDate
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @LastModifiedDate
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    /**
     * 无参构造方法（JPA 要求）。
     */
    public SysBusinessType() {
        /* for JPA */
    }

    // ===== Getters =====

    /**
     * 获取业务类型唯一标识。
     *
     * @return 业务类型 ID (UUID 32位)
     */
    public String getTypeId() {
        return typeId;
    }

    /**
     * 获取业务类型名称。
     *
     * @return 类型名称
     */
    public String getTypeName() {
        return typeName;
    }

    /**
     * 获取业务类型编码。
     *
     * @return 类型编码（唯一）
     */
    public String getTypeCode() {
        return typeCode;
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
     * 获取业务类型状态。
     *
     * @return 类型状态枚举
     */
    public BusinessTypeStatus getTypeStatus() {
        return typeStatus;
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
     * 设置业务类型唯一标识。
     *
     * @param typeId 业务类型 ID
     */
    public void setTypeId(final String typeId) {
        this.typeId = typeId;
    }

    /**
     * 设置业务类型名称。
     *
     * @param typeName 类型名称
     */
    public void setTypeName(final String typeName) {
        this.typeName = typeName;
    }

    /**
     * 设置业务类型编码。
     *
     * @param typeCode 类型编码
     */
    public void setTypeCode(final String typeCode) {
        this.typeCode = typeCode;
    }

    /**
     * 设置排序号。
     *
     * @param sortOrder 排序号
     */
    public void setSortOrder(final Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    /**
     * 设置业务类型状态。
     *
     * @param typeStatus 类型状态枚举
     */
    public void setTypeStatus(final BusinessTypeStatus typeStatus) {
        this.typeStatus = typeStatus;
    }
}
