package com.puchain.fep.web.sysmgmt.config.enterprise.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 企业主体创建/更新请求 DTO。
 *
 * <p>参见 PRD v1.3 §5.10.7.3 企业主体管理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class EnterpriseCreateRequest {

    /** 企业名称，必填，最大 200 字符。 */
    @NotBlank
    @Size(max = 200)
    private String enterpriseName;

    /** 统一社会信用代码，必填，18位数字或大写字母。 */
    @NotBlank
    @Pattern(regexp = "^[0-9A-Z]{18}$", message = "统一社会信用代码必须为 18 位数字或大写字母")
    private String usci;

    /** 报文内容类型，可选，最大 100 字符。 */
    @Size(max = 100)
    private String contentType;

    /** 客户端标识，可选，最大 100 字符。 */
    @Size(max = 100)
    private String clientId;

    /** 密钥参数元数据（非实际密钥内容），可选，最大 500 字符。 */
    @Size(max = 500)
    private String keyParams;

    /** 签名文件路径元数据（非实际文件内容），可选，最大 500 字符。 */
    @Size(max = 500)
    private String signFilePath;

    /**
     * 获取企业名称。
     *
     * @return 企业名称
     */
    public String getEnterpriseName() {
        return enterpriseName;
    }

    /**
     * 设置企业名称。
     *
     * @param enterpriseName 企业名称
     */
    public void setEnterpriseName(final String enterpriseName) {
        this.enterpriseName = enterpriseName;
    }

    /**
     * 获取统一社会信用代码。
     *
     * @return USCI
     */
    public String getUsci() {
        return usci;
    }

    /**
     * 设置统一社会信用代码。
     *
     * @param usci 18位 USCI
     */
    public void setUsci(final String usci) {
        this.usci = usci;
    }

    /**
     * 获取报文内容类型。
     *
     * @return 内容类型（可为 null）
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * 设置报文内容类型。
     *
     * @param contentType 内容类型
     */
    public void setContentType(final String contentType) {
        this.contentType = contentType;
    }

    /**
     * 获取客户端标识。
     *
     * @return 客户端标识（可为 null）
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * 设置客户端标识。
     *
     * @param clientId 客户端标识
     */
    public void setClientId(final String clientId) {
        this.clientId = clientId;
    }

    /**
     * 获取密钥参数元数据。
     *
     * @return 密钥参数描述（可为 null）
     */
    public String getKeyParams() {
        return keyParams;
    }

    /**
     * 设置密钥参数元数据。
     *
     * @param keyParams 密钥参数描述
     */
    public void setKeyParams(final String keyParams) {
        this.keyParams = keyParams;
    }

    /**
     * 获取签名文件路径元数据。
     *
     * @return 签名文件路径（可为 null）
     */
    public String getSignFilePath() {
        return signFilePath;
    }

    /**
     * 设置签名文件路径元数据。
     *
     * @param signFilePath 签名文件路径
     */
    public void setSignFilePath(final String signFilePath) {
        this.signFilePath = signFilePath;
    }
}
