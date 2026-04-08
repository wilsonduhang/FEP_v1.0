package com.puchain.fep.web.sysmgmt.config.outputtype.domain;

import com.puchain.fep.common.domain.EnableDisableStatus;
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
 * 输出类型 Entity，映射 t_sys_output_type 表。
 *
 * <p>参见 PRD v1.3 §5.10.7.2e 输出类型管理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Entity
@Table(name = "t_sys_output_type")
@EntityListeners(AuditingEntityListener.class)
public class SysOutputType {

    @Id
    @Column(name = "output_type_id", length = 32)
    private String outputTypeId;

    @Column(name = "type_name", nullable = false, length = 100)
    private String typeName;

    @Column(name = "type_code", nullable = false, length = 50, unique = true)
    private String typeCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_status", nullable = false, length = 20)
    private EnableDisableStatus typeStatus;

    @CreatedDate
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @LastModifiedDate
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    /**
     * 无参构造方法（JPA 要求）。
     */
    public SysOutputType() {
        /* for JPA */
    }

    // ===== Getters =====

    /**
     * 获取输出类型唯一标识。
     *
     * @return 输出类型 ID (UUID 32位)
     */
    public String getOutputTypeId() {
        return outputTypeId;
    }

    /**
     * 获取输出类型名称。
     *
     * @return 类型名称
     */
    public String getTypeName() {
        return typeName;
    }

    /**
     * 获取输出类型编码。
     *
     * @return 类型编码（唯一）
     */
    public String getTypeCode() {
        return typeCode;
    }

    /**
     * 获取输出类型状态。
     *
     * @return 类型状态枚举
     */
    public EnableDisableStatus getTypeStatus() {
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
     * 设置输出类型唯一标识。
     *
     * @param outputTypeId 输出类型 ID
     */
    public void setOutputTypeId(final String outputTypeId) {
        this.outputTypeId = outputTypeId;
    }

    /**
     * 设置输出类型名称。
     *
     * @param typeName 类型名称
     */
    public void setTypeName(final String typeName) {
        this.typeName = typeName;
    }

    /**
     * 设置输出类型编码。
     *
     * @param typeCode 类型编码
     */
    public void setTypeCode(final String typeCode) {
        this.typeCode = typeCode;
    }

    /**
     * 设置输出类型状态。
     *
     * @param typeStatus 类型状态枚举
     */
    public void setTypeStatus(final EnableDisableStatus typeStatus) {
        this.typeStatus = typeStatus;
    }
}
