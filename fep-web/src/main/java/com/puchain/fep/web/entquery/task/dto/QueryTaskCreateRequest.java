package com.puchain.fep.web.entquery.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 查询任务创建请求 DTO。
 *
 * <p>参见 PRD v1.3 §5.4 企业信息查询管理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class QueryTaskCreateRequest {

    /** 发起查询的企业 ID，必填。 */
    @NotBlank
    private String enterpriseId;

    /** 查询类型，必填，REALTIME 或 BATCH。 */
    @NotBlank
    @Pattern(regexp = "^(REALTIME|BATCH)$", message = "查询类型必须为 REALTIME 或 BATCH")
    private String queryType;

    /** 被查询企业 USCI，必填，18位数字或大写字母。 */
    @NotBlank
    @Pattern(regexp = "^[0-9A-Z]{18}$", message = "USCI 必须为 18 位数字或大写字母")
    private String usci;

    /** 被查询企业名称，可选，最大 200 字符。 */
    @Size(max = 200)
    private String queryTargetName;

    /** 批量查询文件路径，可选，最大 500 字符。 */
    @Size(max = 500)
    private String batchFilePath;

    /**
     * 获取发起查询的企业 ID。
     *
     * @return 企业 ID
     */
    public String getEnterpriseId() {
        return enterpriseId;
    }

    /**
     * 设置发起查询的企业 ID。
     *
     * @param enterpriseId 企业 ID
     */
    public void setEnterpriseId(final String enterpriseId) {
        this.enterpriseId = enterpriseId;
    }

    /**
     * 获取查询类型。
     *
     * @return 查询类型字符串（REALTIME/BATCH）
     */
    public String getQueryType() {
        return queryType;
    }

    /**
     * 设置查询类型。
     *
     * @param queryType 查询类型（REALTIME/BATCH）
     */
    public void setQueryType(final String queryType) {
        this.queryType = queryType;
    }

    /**
     * 获取被查询企业 USCI。
     *
     * @return 18位 USCI
     */
    public String getUsci() {
        return usci;
    }

    /**
     * 设置被查询企业 USCI。
     *
     * @param usci 18位 USCI
     */
    public void setUsci(final String usci) {
        this.usci = usci;
    }

    /**
     * 获取被查询企业名称。
     *
     * @return 企业名称（可为 null）
     */
    public String getQueryTargetName() {
        return queryTargetName;
    }

    /**
     * 设置被查询企业名称。
     *
     * @param queryTargetName 企业名称
     */
    public void setQueryTargetName(final String queryTargetName) {
        this.queryTargetName = queryTargetName;
    }

    /**
     * 获取批量查询文件路径。
     *
     * @return 文件路径（可为 null）
     */
    public String getBatchFilePath() {
        return batchFilePath;
    }

    /**
     * 设置批量查询文件路径。
     *
     * @param batchFilePath 文件路径
     */
    public void setBatchFilePath(final String batchFilePath) {
        this.batchFilePath = batchFilePath;
    }
}
