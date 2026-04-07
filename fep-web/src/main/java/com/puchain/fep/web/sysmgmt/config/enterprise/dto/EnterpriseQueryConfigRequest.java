package com.puchain.fep.web.sysmgmt.config.enterprise.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 企业精准查询配置更新请求 DTO。
 *
 * <p>参见 PRD v1.3 §5.10.7.3 企业主体管理（Tab 3 精准查询配置）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class EnterpriseQueryConfigRequest {

    /** 查询类型，必填，最大 50 字符。 */
    @NotBlank(message = "查询类型不能为空")
    @Size(max = 50, message = "查询类型最长 50 字符")
    private String queryType;

    /** 查询参数 JSON，可选。 */
    private String queryParams;

    /**
     * 获取查询类型。
     *
     * @return 查询类型
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
}
