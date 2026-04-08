package com.puchain.fep.web.sysmgmt.config.businesstype.dto;

import com.puchain.fep.common.domain.EnableDisableStatus;
import com.puchain.fep.web.sysmgmt.config.businesstype.domain.SysBusinessType;

import java.time.LocalDateTime;

/**
 * 业务类型响应 DTO。
 *
 * <p>参见 PRD v1.3 §5.10.7.2a 业务类型管理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class BusinessTypeResponse {

    private String typeId;
    private String typeName;
    private String typeCode;
    private Integer sortOrder;
    private EnableDisableStatus typeStatus;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /**
     * 从 SysBusinessType Entity 构建响应 DTO。
     *
     * @param entity 业务类型 Entity
     * @return 响应 DTO
     */
    public static BusinessTypeResponse from(final SysBusinessType entity) {
        BusinessTypeResponse resp = new BusinessTypeResponse();
        resp.setTypeId(entity.getTypeId());
        resp.setTypeName(entity.getTypeName());
        resp.setTypeCode(entity.getTypeCode());
        resp.setSortOrder(entity.getSortOrder());
        resp.setTypeStatus(entity.getTypeStatus());
        resp.setCreateTime(entity.getCreateTime());
        resp.setUpdateTime(entity.getUpdateTime());
        return resp;
    }

    /**
     * 获取业务类型 ID。
     *
     * @return 类型 ID
     */
    public String getTypeId() {
        return typeId;
    }

    /**
     * 设置业务类型 ID。
     *
     * @param typeId 类型 ID
     */
    public void setTypeId(final String typeId) {
        this.typeId = typeId;
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
     * 获取排序号。
     *
     * @return 排序号
     */
    public Integer getSortOrder() {
        return sortOrder;
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
     * 获取类型状态。
     *
     * @return 类型状态枚举
     */
    public EnableDisableStatus getTypeStatus() {
        return typeStatus;
    }

    /**
     * 设置类型状态。
     *
     * @param typeStatus 类型状态枚举
     */
    public void setTypeStatus(final EnableDisableStatus typeStatus) {
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
