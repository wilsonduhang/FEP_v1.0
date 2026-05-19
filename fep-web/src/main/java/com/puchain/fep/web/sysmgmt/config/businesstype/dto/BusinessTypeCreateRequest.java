package com.puchain.fep.web.sysmgmt.config.businesstype.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 业务类型创建/更新请求 DTO。
 *
 * <p>参见 PRD v1.3 §5.10.7.2a 业务类型管理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class BusinessTypeCreateRequest {

    @NotBlank(message = "类型名称不能为空")
    @Size(min = 1, max = 30, message = "类型名称长度 1-30 字符")
    private String typeName;

    @NotBlank(message = "类型编码不能为空")
    @Size(max = 50, message = "类型编码最长 50 字符")
    private String typeCode;

    @NotNull(message = "排序号不能为空")
    private Integer sortOrder;

    private List<String> msgNos;

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
     * 获取关联 inbound 报文号列表（4 位数字，可为空）。
     *
     * @return msgNos
     */
    public List<String> getMsgNos() {
        return msgNos;
    }

    /**
     * 设置关联 inbound 报文号列表。
     *
     * @param msgNos 报文号列表（4 位数字），可为 null 或空
     */
    public void setMsgNos(final List<String> msgNos) {
        this.msgNos = msgNos;
    }
}
