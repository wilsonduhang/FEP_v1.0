package com.puchain.fep.web.sysmgmt.config.outputtype.dto;

import com.puchain.fep.common.domain.EnableDisableStatus;
import com.puchain.fep.web.sysmgmt.config.outputtype.domain.SysOutputType;

import java.time.LocalDateTime;

/**
 * 输出类型响应 DTO。
 *
 * <p>参见 PRD v1.3 §5.10.7.2e 输出类型管理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class OutputTypeResponse {

    private String outputTypeId;
    private String typeName;
    private String typeCode;
    private EnableDisableStatus typeStatus;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /**
     * 从 SysOutputType Entity 构建响应 DTO。
     *
     * @param entity 输出类型 Entity
     * @return 响应 DTO
     */
    public static OutputTypeResponse from(final SysOutputType entity) {
        OutputTypeResponse resp = new OutputTypeResponse();
        resp.setOutputTypeId(entity.getOutputTypeId());
        resp.setTypeName(entity.getTypeName());
        resp.setTypeCode(entity.getTypeCode());
        resp.setTypeStatus(entity.getTypeStatus());
        resp.setCreateTime(entity.getCreateTime());
        resp.setUpdateTime(entity.getUpdateTime());
        return resp;
    }

    /**
     * 获取输出类型 ID。
     *
     * @return 输出类型 ID
     */
    public String getOutputTypeId() {
        return outputTypeId;
    }

    /**
     * 设置输出类型 ID。
     *
     * @param outputTypeId 输出类型 ID
     */
    public void setOutputTypeId(final String outputTypeId) {
        this.outputTypeId = outputTypeId;
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
