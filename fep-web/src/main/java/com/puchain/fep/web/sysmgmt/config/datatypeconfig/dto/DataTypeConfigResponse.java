package com.puchain.fep.web.sysmgmt.config.datatypeconfig.dto;

import com.puchain.fep.web.sysmgmt.config.datatypeconfig.domain.DataTypeConfigStatus;
import com.puchain.fep.web.sysmgmt.config.datatypeconfig.domain.SysDataTypeConfig;

import java.time.LocalDateTime;

/**
 * 数据类型响应 DTO。
 *
 * <p>参见 PRD v1.3 §5.10.7.2f 数据类型管理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class DataTypeConfigResponse {

    private String dataTypeId;
    private String typeName;
    private String typeCode;
    private DataTypeConfigStatus typeStatus;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /**
     * 从 SysDataTypeConfig Entity 构建响应 DTO。
     *
     * @param entity 数据类型 Entity
     * @return 响应 DTO
     */
    public static DataTypeConfigResponse from(final SysDataTypeConfig entity) {
        DataTypeConfigResponse resp = new DataTypeConfigResponse();
        resp.setDataTypeId(entity.getDataTypeId());
        resp.setTypeName(entity.getTypeName());
        resp.setTypeCode(entity.getTypeCode());
        resp.setTypeStatus(entity.getTypeStatus());
        resp.setCreateTime(entity.getCreateTime());
        resp.setUpdateTime(entity.getUpdateTime());
        return resp;
    }

    /**
     * 获取数据类型 ID。
     *
     * @return 数据类型 ID
     */
    public String getDataTypeId() {
        return dataTypeId;
    }

    /**
     * 设置数据类型 ID。
     *
     * @param dataTypeId 数据类型 ID
     */
    public void setDataTypeId(final String dataTypeId) {
        this.dataTypeId = dataTypeId;
    }

    /**
     * 获取类型名称。
     *
     * @return 类型名称
     */
    public String getTypeName() {
        return typeName;
    }

    /**
     * 设置类型名称。
     *
     * @param typeName 类型名称
     */
    public void setTypeName(final String typeName) {
        this.typeName = typeName;
    }

    /**
     * 获取类型编码。
     *
     * @return 类型编码
     */
    public String getTypeCode() {
        return typeCode;
    }

    /**
     * 设置类型编码。
     *
     * @param typeCode 类型编码
     */
    public void setTypeCode(final String typeCode) {
        this.typeCode = typeCode;
    }

    /**
     * 获取类型状态。
     *
     * @return 类型状态枚举
     */
    public DataTypeConfigStatus getTypeStatus() {
        return typeStatus;
    }

    /**
     * 设置类型状态。
     *
     * @param typeStatus 类型状态枚举
     */
    public void setTypeStatus(final DataTypeConfigStatus typeStatus) {
        this.typeStatus = typeStatus;
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
