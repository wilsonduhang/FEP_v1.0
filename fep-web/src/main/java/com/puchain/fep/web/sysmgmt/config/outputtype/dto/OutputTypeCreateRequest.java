package com.puchain.fep.web.sysmgmt.config.outputtype.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 输出类型创建/更新请求 DTO。
 *
 * <p>参见 PRD v1.3 §5.10.7.2e 输出类型管理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class OutputTypeCreateRequest {

    @NotBlank(message = "类型名称不能为空")
    @Size(min = 1, max = 100, message = "类型名称长度 1-100 字符")
    private String typeName;

    @NotBlank(message = "类型编码不能为空")
    @Size(max = 50, message = "类型编码最长 50 字符")
    private String typeCode;

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
}
