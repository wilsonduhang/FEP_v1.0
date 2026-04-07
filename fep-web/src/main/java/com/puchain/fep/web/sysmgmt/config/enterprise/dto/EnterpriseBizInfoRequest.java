package com.puchain.fep.web.sysmgmt.config.enterprise.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 企业业务信息关联创建请求 DTO。
 *
 * <p>参见 PRD v1.3 §5.10.7.3 企业主体管理（Tab 2 业务信息）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class EnterpriseBizInfoRequest {

    /** 业务类型 ID，必填。 */
    @NotBlank(message = "业务类型ID不能为空")
    private String businessTypeId;

    /** 配置 JSON，可选。 */
    private String configJson;

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
}
